/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.commands.util.InviteCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Some architectural notes:
 * Issued commands will always go through here. It is their own job to find out for which game they have been issued,
 * and make the appropriate calls or handle any user errors
 */
@Component
public class CommandHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommandHandler.class);

    public void handleMessage(final CommRegistry commRegistry, @Nonnull final MessageReceivedEvent event) {
        //ignore bot accounts generally
        if (event.getAuthor().isBot()) {
            return;
        }

        //ignore channels where we don't have sending permissions, with a special exception for the help command
        if (event.getTextChannel() != null && !event.getTextChannel().canTalk()
                && !event.getMessage().getContentRaw().toLowerCase().startsWith((WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER).toLowerCase())) {
            return;
        }

        //update user stats
        final Game g = Games.get(event.getChannel().getIdLong());
        if (g != null) g.userPosted(event.getMessage());


        final CommandContext context;
        try {
            context = CommandContext.parse(commRegistry, event);
        } catch (final DatabaseException e) {
            log.error("Db blew up parsing a command", e);
            return;
        }
        if (context == null) {
            return;
        }

        //filter for _special_ ppl in the Wolfia guild
        final GuildCommandContext guildContext = context.requireGuild(false);
        if (guildContext != null && guildContext.guild.getIdLong() == App.WOLFIA_LOUNGE_ID) {
            final Category parent = guildContext.getTextChannel().getParent();
            //noinspection StatementWithEmptyBody
            if (guildContext.getTextChannel().getIdLong() == WolfiaGuildListener.SPAM_CHANNEL_ID //spam channel is k
                    || (parent != null && parent.getIdLong() == WolfiaGuildListener.GAME_CATEGORY_ID) //game channels are k
                    || context.invoker.getIdLong() == App.OWNER_ID) { //owner is k
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

        handleCommand(context);
    }

    /**
     * @param context
     *         the parsed input of a user
     */
    public void handleCommand(@Nonnull final CommandContext context) {
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
            context.invoke();
        } catch (final UserFriendlyException e) {
            context.reply("There was a problem executing your command:\n" + e.getMessage());
        } catch (final IllegalGameStateException e) {
            context.reply(e.getMessage());
        } catch (final DatabaseException e) {
            log.error("Db blew up while handling command", e);
            context.reply("The database is not available currently. Please try again later. Sorry for the inconvenience!");
        } catch (final Exception e) {
            try {
                final MessageReceivedEvent ev = context.event;
                Throwable t = e;
                while (t != null) {
                    String inviteLink = "";
                    try {
                        final TextChannel tc = ev.getTextChannel();
                        if (tc == null) {
                            inviteLink = "PRIVATE";
                        } else {
                            inviteLink = TextchatUtils.getOrCreateInviteLinkForGuild(tc.getGuild(), tc);
                        }
                    } catch (final Exception ex) {
                        log.error("Exception during exception handling of command creating an invite", ex);
                    }
                    log.error("Exception `{}` while handling a command in guild {}, channel {}, user {}, invite {}",
                            t.getMessage(), ev.getGuild(), ev.getChannel().getIdLong(),
                            ev.getAuthor().getIdLong(), inviteLink, t);
                    t = t.getCause();
                }
                RestActions.sendMessage(ev.getTextChannel(),
                        String.format("%s, an internal exception happened while executing your command:"
                                        + "\n`%s`"
                                        + "\nSorry about that. The issue has been logged and will hopefully be fixed soon."
                                        + "\nIf you want to help solve this as fast as possible, please join our support guild."
                                        + "\nSay `%s` to receive an invite.",
                                ev.getAuthor().getAsMention(), context.msg.getContentRaw(), WolfiaConfig.DEFAULT_PREFIX + InviteCommand.TRIGGER));
            } catch (final Exception ex) {
                log.error("Exception during exception handling of command", ex);
            }
        }
    }
}
