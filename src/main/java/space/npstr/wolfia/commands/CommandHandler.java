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
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.entities.stats.CommandStats;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.RejectedExecutionException;
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

//    public static final String COMM_TRIGGER_HELP = "help";
//
//
//    private static final Map<String, BaseCommand> COMMAND_REGISTRY = new HashMap<>();
//
//    private static void registerCommand(final BaseCommand command) {
//        for (final String trigger : command.aliases) {
//            if (COMMAND_REGISTRY.containsKey(trigger)) {
//                log.error("Duplicate command trigger: {}", trigger);
//            }
//            COMMAND_REGISTRY.put(trigger, command);
//        }
//    }
//
//    static {
//        //@formatter:off
//
//        //game related commands
//        registerCommand(new InCommand                        ("in", "join"));
//        registerCommand(new OutCommand                       ("out", "leave"));
//        registerCommand(new RolePmCommand                    ("rolepm", "rpm"));
//        registerCommand(new SetupCommand                     ("setup"));
//        registerCommand(new StartCommand                     ("start"));
//        registerCommand(new StatusCommand                    ("status"));
//
//        //ingame commands
//        registerCommand(new ShootCommand                     ("shoot", "s", "blast"));
//        registerCommand(new VoteCommand                      ("vote", "v", "lynch"));
//        registerCommand(new UnvoteCommand                    ("unvote", "u", "uv"));
//        registerCommand(new CheckCommand                     ("check"));
//        registerCommand(new VoteCountCommand                 ("votecount", "vc"));
//        registerCommand(new NightkillCommand                 ("nightkill", "nk"));
//
//        //stats commands
//        registerCommand(new BotStatsCommand                  ("botstats"));
//        registerCommand(new GuildStatsCommand                ("guildstats"));
//        registerCommand(new UserStatsCommand                 ("userstats"));
//
//        //util commands
//        registerCommand(new ChannelSettingsCommand           ("channelsettings", "cs"));
//        registerCommand(new CommandsCommand                  ("commands", "comms"));
//        registerCommand(new HelpCommand                      (COMM_TRIGGER_HELP));
//        registerCommand(new InfoCommand                      ("info"));
//        registerCommand(new ReplayCommand                    ("replay"));
//        registerCommand(new TagCommand                       ("tag"));
//
//        //bot owner/debug commands
//        registerCommand(new BanCommand                       ("ban"));
//        registerCommand(new DbTestCommand                    ("dbtest"));
//        registerCommand(new EvalCommand                      ("eval"));
//        registerCommand(new KillGameCommand                  ("killgame"));
//        registerCommand(new MaintenanceCommand               ("maint"));
//        registerCommand(new RegisterPrivateServerCommand     ("register"));
//        registerCommand(new RestartCommand                   ("restart"));
//        registerCommand(new RunningCommand                   ("running"));
//        registerCommand(new ShutdownCommand                  ("shutdown"));
//        registerCommand(new SyncCommand                      ("sync"));
//
//        //@formatter:on
//    }
//
//    public static BaseCommand getCommand(final String input) {
//        return COMMAND_REGISTRY.get(input);
//    }
//
//    public static String mainTrigger(final Class<? extends BaseCommand> clazz) {
//        for (final BaseCommand command : COMMAND_REGISTRY.values()) {
//            if (clazz.isInstance(command)) {
//                return command.name;
//            }
//        }
//        log.error("Command {} is not registered in the commandhandler, can't find a main trigger for it", clazz.getSimpleName());
//        return "";
//    }

    /**
     * @param context the parsed input of a user
     * @param filter  a filter, so that the source of the call can control which commands should be handled (or not)
     *                the predicate should return true if the command should be handled and false otherwise
     */
    public static void handleCommand(@Nonnull final CommandContext context, @Nonnull final Predicate<BaseCommand> filter) {
        try {
            if (context.command instanceof IOwnerRestricted && !context.isOwner()) {
                //not the bot owner
                log.info("user {}, channel {}, attempted issuing owner restricted command: {}",
                        context.invoker, context.channel, context.msg.getRawContent());
                return;
            }
            if (!filter.test(context.command)) {
                log.info("user {}, channel {}, command: {} was filtered and will not be executed",
                        context.invoker, context.channel, context.msg.getRawContent());
                return;
            }
            log.info("user {}, channel {}, command {} about to be executed",
                    context.invoker, context.channel, context.msg.getRawContent());
            final boolean success = context.invoke();
            final long executed = System.currentTimeMillis();
            try {
                Wolfia.executor.submit(() -> Wolfia.getDbWrapper().persist(new CommandStats(context, executed, success)));
            } catch (final RejectedExecutionException ignored) {
                //may happen on shutdown
            }
        } catch (final UserFriendlyException e) {
            context.reply("There was a problem executing your command:\n" + e.getMessage());
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
                        inviteLink = TextchatUtils.getOrCreateInviteLinkForGuild(ev.getGuild(), ev.getTextChannel());
                    } catch (final Exception ex) {
                        log.error("Exception during exception handling of command creating an invite", ex);
                    }
                    log.error("Exception `{}` while handling a command in guild {}, channel {}, user {}, invite {}",
                            t.getMessage(), ev.getGuild().getIdLong(), ev.getChannel().getIdLong(),
                            ev.getAuthor().getIdLong(), inviteLink, t);
                    t = t.getCause();
                }
                RestActions.sendMessage(ev.getTextChannel(),
                        String.format("%s, an internal exception happened while executing your command:"
                                        + "\n`%s`"
                                        + "\nSorry about that. The issue has been logged and will hopefully be fixed soon."
                                        + "\nIf you want to help solve this as fast as possible, please join our support guild."
                                        + "\nSay `%s` to receive an invite.",
                                ev.getAuthor().getAsMention(), context.msg.getRawContent(), Config.PREFIX + CommRegistry.COMM_TRIGGER_INVITE));
            } catch (final Exception ex) {
                log.error("Exception during exception handling of command", ex);
            }
        }
    }
}
