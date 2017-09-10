/*
 * Copyright (C) 2017 Dennis Neufeld
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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.debug.BanCommand;
import space.npstr.wolfia.commands.debug.DbTestCommand;
import space.npstr.wolfia.commands.debug.EvalCommand;
import space.npstr.wolfia.commands.debug.KillGameCommand;
import space.npstr.wolfia.commands.debug.MaintenanceCommand;
import space.npstr.wolfia.commands.debug.RegisterPrivateServerCommand;
import space.npstr.wolfia.commands.debug.RunningCommand;
import space.npstr.wolfia.commands.debug.ShutdownCommand;
import space.npstr.wolfia.commands.debug.UpdateCommand;
import space.npstr.wolfia.commands.game.InCommand;
import space.npstr.wolfia.commands.game.OutCommand;
import space.npstr.wolfia.commands.game.RolePmCommand;
import space.npstr.wolfia.commands.game.SetupCommand;
import space.npstr.wolfia.commands.game.StartCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.commands.ingame.CheckCommand;
import space.npstr.wolfia.commands.ingame.NightkillCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.commands.ingame.UnvoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.commands.stats.BotStatsCommand;
import space.npstr.wolfia.commands.stats.GuildStatsCommand;
import space.npstr.wolfia.commands.stats.UserStatsCommand;
import space.npstr.wolfia.commands.util.ChannelSettingsCommand;
import space.npstr.wolfia.commands.util.CommandsCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.commands.util.InfoCommand;
import space.npstr.wolfia.commands.util.ReplayCommand;
import space.npstr.wolfia.commands.util.TagCommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.stats.CommandStats;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Some architectural notes:
 * Issued commands will always go through here. It is their own job to find out for which game they have been issued,
 * and make the appropriate calls or handle any user errors
 */
public class CommandHandler {

    private final static Logger log = LoggerFactory.getLogger(CommandHandler.class);

    private static final Map<String, BaseCommand> COMMAND_REGISTRY = new HashMap<>();

    private static void registerCommand(final BaseCommand command) {
        for (final String trigger : command.commandTriggers()) {
            if (COMMAND_REGISTRY.containsKey(trigger)) {
                log.error("Duplicate command trigger: {}", trigger);
            }
            COMMAND_REGISTRY.put(trigger, command);
        }
    }

    static {
        //@formatter:off

        //game related commands
        registerCommand(new InCommand                        ("in", "join"));
        registerCommand(new OutCommand                       ("out", "leave"));
        registerCommand(new RolePmCommand                    ("rolepm", "rpm"));
        registerCommand(new SetupCommand                     ("setup"));
        registerCommand(new StartCommand                     ("start"));
        registerCommand(new StatusCommand                    ("status"));

        //ingame commands
        registerCommand(new ShootCommand                     ("shoot", "s", "blast"));
        registerCommand(new VoteCommand                      ("vote", "v", "lynch"));
        registerCommand(new UnvoteCommand                    ("unvote", "u", "uv"));
        registerCommand(new CheckCommand                     ("check"));
        registerCommand(new VoteCountCommand                 ("votecount", "vc"));
        registerCommand(new NightkillCommand                 ("nightkill", "nk"));

        //stats commands
        registerCommand(new BotStatsCommand                  ("botstats"));
        registerCommand(new GuildStatsCommand                ("guildstats"));
        registerCommand(new UserStatsCommand                 ("userstats"));

        //util commands
        registerCommand(new ChannelSettingsCommand           ("channelsettings", "cs"));
        registerCommand(new CommandsCommand                  ("commands", "comms"));
        registerCommand(new HelpCommand                      ("help"));
        registerCommand(new InfoCommand                      ("info"));
        registerCommand(new ReplayCommand                    ("replay"));
        registerCommand(new TagCommand                       ("tag"));

        //bot owner/debug commands
        registerCommand(new BanCommand                       ("ban"));
        registerCommand(new DbTestCommand                    ("dbtest"));
        registerCommand(new EvalCommand                      ("eval"));
        registerCommand(new KillGameCommand                  ("killgame"));
        registerCommand(new MaintenanceCommand               ("maint"));
        registerCommand(new RegisterPrivateServerCommand     ("register"));
        registerCommand(new RunningCommand                   ("running"));
        registerCommand(new ShutdownCommand                  ("shutdown"));
        registerCommand(new UpdateCommand                    ("update"));

        //@formatter:on
    }

    public static BaseCommand getCommand(final String input) {
        return COMMAND_REGISTRY.get(input);
    }

    public static String mainTrigger(final Class<? extends BaseCommand> clazz) {
        for (final BaseCommand command : COMMAND_REGISTRY.values()) {
            if (clazz.isInstance(command)) {
                return command.getMainTrigger();
            }
        }
        log.error("Command {} is not registered in the commandhandler, can't find a main trigger for it", clazz.getSimpleName());
        return "";
    }

    /**
     * @param commandInfo the parsed input of a user
     * @param filter      a filter, so that the source of the call can control which commands should be handled (or not)
     *                    the predicate should return true if the command should be handled and false otherwise
     */
    public static void handleCommand(final CommandParser.CommandContainer commandInfo, final Predicate<BaseCommand> filter) {
        try {
            final BaseCommand command = COMMAND_REGISTRY.get(commandInfo.command);
            if (command == null) {
                //unknown command
                log.info("user {}, channel {}, unknown command issued: {}",
                        commandInfo.event.getAuthor().getIdLong(),
                        commandInfo.event.getChannel().getIdLong(),
                        commandInfo.raw);
                return;
            }

            if (command instanceof IOwnerRestricted && !App.isOwner(commandInfo.event.getAuthor())) {
                //not the bot owner
                log.info("user {}, channel {}, attempted issuing owner restricted command: {}",
                        commandInfo.event.getAuthor().getIdLong(),
                        commandInfo.event.getChannel().getIdLong(),
                        commandInfo.raw);
                return;
            }
            if (!filter.test(command)) {
                log.info("user {}, channel {}, command: {} was filtered and will not be executed",
                        commandInfo.event.getAuthor().getIdLong(),
                        commandInfo.event.getChannel().getIdLong(),
                        commandInfo.raw);
                return;
            }
            log.info("user {}, channel {}, command {} about to be executed",
                    commandInfo.event.getAuthor().getIdLong(), commandInfo.event.getChannel().getIdLong(),
                    commandInfo.event.getMessage().getRawContent());
            final boolean success = command.execute(commandInfo);
            final long executed = System.currentTimeMillis();
            Wolfia.submit(() -> DbWrapper.persist(new CommandStats(commandInfo, command.getClass(), executed, success)));
        } catch (final UserFriendlyException e) {
            Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(), "There was a problem executing your command:\n%s", e.getMessage());
        } catch (final Exception e) {
            try {
                final MessageReceivedEvent ev = commandInfo.event;
                Throwable t = e;
                while (t != null) {
                    String inviteLink = "";
                    try {
                        inviteLink = TextchatUtils.getOrCreateInviteLinkForGuild(ev.getGuild(), ev.getTextChannel());
                    } catch (final Exception ex) {
                        log.error("Exception during exception handling of command creating an invite", ex);
                    }
                    log.error("Exception `{}` while handling a command in guild {}, channel {}, user {}, invite {}",
                            t.getMessage(), ev.getGuild().getIdLong(), ev.getChannel().getIdLong(),
                            ev.getAuthor().getIdLong(), inviteLink, t);
                    t = t.getCause();
                }
                Wolfia.handleOutputMessage(ev.getTextChannel(),
                        "%s, an internal exception happened while executing your command\n`%s`\nSorry about that. Please " +
                                "contact the developer through the website or Discord guild sent to you through `%s`",
                        ev.getAuthor().getAsMention(), commandInfo.raw, Config.PREFIX + mainTrigger(HelpCommand.class));
            } catch (final Exception ex) {
                log.error("Exception during exception handling of command", ex);
            }
        }
    }
}
