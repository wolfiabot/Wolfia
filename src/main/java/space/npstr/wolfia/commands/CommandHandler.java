/*
 * Copyright (C) 2016-2020 the original author or authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.commands;

import io.prometheus.client.Summary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jooq.exception.DataAccessException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.game.StartCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.commands.util.InviteCommand;
import space.npstr.wolfia.commands.util.TagCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.domain.privacy.PrivacyService;
import space.npstr.wolfia.domain.settings.ChannelSettings;
import space.npstr.wolfia.domain.settings.ChannelSettingsService;
import space.npstr.wolfia.domain.setup.InCommand;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.system.ApplicationInfoProvider;
import space.npstr.wolfia.system.metrics.MetricsRegistry;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import static io.prometheus.client.Summary.Timer;

/**
 * Some architectural notes:
 * Issued commands will always go through here. It is their own job to find out for which game they have been issued,
 * and make the appropriate calls or handle any user errors
 */
@Component
public class CommandHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommandHandler.class);

    private final GameRegistry gameRegistry;
    private final CommandContextParser commandContextParser;
    private final CommRegistry commRegistry;
    private final ChannelSettingsService channelSettingsService;
    private final PrivacyService privacyService;

    public CommandHandler(GameRegistry gameRegistry, CommandContextParser commandContextParser,
                          CommRegistry commRegistry, ChannelSettingsService channelSettingsService,
                          PrivacyService privacyService) {

        this.gameRegistry = gameRegistry;
        this.commandContextParser = commandContextParser;
        this.commRegistry = commRegistry;
        this.channelSettingsService = channelSettingsService;
        this.privacyService = privacyService;
    }

    @EventListener
    public void onMessageReceived(MessageReceivedEvent event) {
        Timer received = MetricsRegistry.commandRetentionTime.startTimer();
        //ignore bot accounts generally
        if (event.getAuthor().isBot()) {
            return;
        }

        //ignore channels where we don't have sending permissions, with a special exception for the help command
        if (event.isFromType(ChannelType.TEXT) && !event.getTextChannel().canTalk()
                && !event.getMessage().getContentRaw().toLowerCase().startsWith((WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER).toLowerCase())) {
            return;
        }

        //update user stats
        Game g = this.gameRegistry.get(event.getChannel().getIdLong());
        if (g != null) g.userPosted(event.getMessage());


        CommandContext context = this.commandContextParser.parse(this.commRegistry, event);

        if (context == null) {
            return;
        }

        // this check does a database request so we want it to be further down the check chain.
        // we can put this check behind the user stats processing, because users who dont have data processing enabled,
        // cannot issue commands, so they cannot join games, to their user stats won't be processed
        if (!this.privacyService.isDataProcessingEnabled(event.getAuthor().getIdLong())) {
            return;
        }

        BaseCommand command = context.command;
        if (command instanceof GameCommand
                || command instanceof StartCommand
                || command instanceof InCommand
                || command instanceof TagCommand) {
            ChannelSettings channelSettings = this.channelSettingsService.channel(context.getChannel().getIdLong()).getOrDefault();
            if (!channelSettings.isGameChannel()) {
                String alternativeChannels = "";
                List<TextChannel> suggestedChannels = suggestGameEnabledChannels(context);
                if (!suggestedChannels.isEmpty()) {
                    String suggestedString = suggestedChannels.stream()
                            .map(IMentionable::getAsMention)
                            .collect(Collectors.joining(", "));
                    alternativeChannels = String.format(" Try %s instead.", suggestedString);
                }
                context.replyWithMention("this channel is not enabled for playing games." + alternativeChannels);
                return;
            }
        }


        //filter for _special_ ppl in the Wolfia guild
        GuildCommandContext guildContext = context.requireGuild(false);
        if (guildContext != null && guildContext.guild.getIdLong() == App.WOLFIA_LOUNGE_ID) {
            Category parent = guildContext.getTextChannel().getParent();
            var appInfoProvider = new ApplicationInfoProvider(event.getJDA().getShardManager());
            //noinspection StatementWithEmptyBody
            if (guildContext.getTextChannel().getIdLong() == WolfiaGuildListener.SPAM_CHANNEL_ID //spam channel is k
                    || (parent != null && parent.getIdLong() == WolfiaGuildListener.GAME_CATEGORY_ID) //game channels are k
                    || appInfoProvider.isOwner(context.getInvoker())) { //owner is k
                //allowed
            } else {
                context.replyWithMention("read the **rules** in <#" + WolfiaGuildListener.RULES_CHANNEL_ID + ">.",
                        message -> RestActions.restService.schedule(
                                () -> RestActions.deleteMessage(message), 5, TimeUnit.SECONDS)
                );
                RestActions.restService.schedule(context::deleteMessage, 5, TimeUnit.SECONDS);
                return;
            }
        }

        handleCommand(context, received);
    }

    private List<TextChannel> suggestGameEnabledChannels(CommandContext context) {
        Optional<Member> memberOpt = context.getMember();
        if (memberOpt.isEmpty()) {
            return List.of();
        }
        Member member = memberOpt.get();
        List<TextChannel> textChannels = new ArrayList<>(member.getGuild().getTextChannels());
        Collections.shuffle(textChannels);
        return textChannels.stream()
                .filter(channel -> channel.canTalk(member))
                .filter(channel -> channelSettingsService.channel(channel.getIdLong()).getOrDefault().isGameChannel())
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * @param context the parsed input of a user
     */
    private void handleCommand(CommandContext context, Timer received) {
        try {
            boolean canCallCommand = context.command instanceof PublicCommand || context.isOwner();
            if (!canCallCommand) {
                //not the bot owner
                log.info("user {}, channel {}, attempted issuing owner restricted command: {}",
                        context.invoker, context.channel, context.msg.getContentRaw());
                return;
            }
            log.info("user {}, channel {}, command {} about to be executed",
                    context.invoker, context.channel, context.msg.getContentRaw());

            received.observeDuration();//retention
            try (Summary.Timer ignored = MetricsRegistry.commandProcessTime.labels(context.command.getClass().getSimpleName()).startTimer()) {
                context.command.execute(context);
            }
        } catch (UserFriendlyException e) {
            context.reply("There was a problem executing your command:\n" + e.getMessage());
        } catch (IllegalGameStateException e) {
            context.reply(e.getMessage());
        } catch (DataAccessException e) {
            log.error("Db blew up while handling command", e);
            context.reply("The database is not available currently. Please try again later. Sorry for the inconvenience!");
        } catch (Exception e) {
            try {
                MessageReceivedEvent ev = context.event;
                Throwable t = e;
                while (t != null) {
                    String inviteLink = "";
                    try {
                        if (ev.isFromType(ChannelType.TEXT)) {
                            TextChannel tc = ev.getTextChannel();
                            inviteLink = TextchatUtils.getOrCreateInviteLinkForGuild(tc.getGuild(), tc);
                        } else {
                            inviteLink = "PRIVATE";
                        }
                    } catch (Exception ex) {
                        log.error("Exception during exception handling of command creating an invite", ex);
                    }
                    String guild = ev.isFromGuild() ? ev.getGuild().toString() : "not a guild";
                    log.error("Exception `{}` while handling a command in guild {}, channel {}, user {}, invite {}",
                            t.getMessage(), guild, ev.getChannel().getIdLong(),
                            ev.getAuthor().getIdLong(), inviteLink, t);
                    t = t.getCause();
                }
                RestActions.sendMessage(ev.getChannel(),
                        String.format("%s, an internal exception happened while executing your command:"
                                        + "\n`%s`"
                                        + "\nSorry about that. The issue has been logged and will hopefully be fixed soon."
                                        + "\nIf you want to help solve this as fast as possible, please join our support guild."
                                        + "\nSay `%s` to receive an invite.",
                                ev.getAuthor().getAsMention(), context.msg.getContentRaw(), WolfiaConfig.DEFAULT_PREFIX + InviteCommand.TRIGGER));
            } catch (Exception ex) {
                log.error("Exception during exception handling of command", ex);
                log.error("Original exception", e);
            }
        }
    }
}
