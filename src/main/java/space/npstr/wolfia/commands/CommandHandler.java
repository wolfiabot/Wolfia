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
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.debug.DbTestCommand;
import space.npstr.wolfia.commands.debug.EvalCommand;
import space.npstr.wolfia.commands.debug.MaintenanceCommand;
import space.npstr.wolfia.commands.debug.RegisterPrivateServerCommand;
import space.npstr.wolfia.commands.debug.ShutdownCommand;
import space.npstr.wolfia.commands.debug.UpdateCommand;
import space.npstr.wolfia.commands.game.InCommand;
import space.npstr.wolfia.commands.game.OutCommand;
import space.npstr.wolfia.commands.game.RolePMCommand;
import space.npstr.wolfia.commands.game.SetupCommand;
import space.npstr.wolfia.commands.game.ShootCommand;
import space.npstr.wolfia.commands.game.StartCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.commands.util.BanCommand;
import space.npstr.wolfia.commands.util.BotStatsCommand;
import space.npstr.wolfia.commands.util.ChannelSettingsCommand;
import space.npstr.wolfia.commands.util.GuildStatsCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.commands.util.InfoCommand;
import space.npstr.wolfia.commands.util.ReplayCommand;
import space.npstr.wolfia.commands.util.TagCommand;
import space.npstr.wolfia.commands.util.UserStatsCommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.stats.CommandStats;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.TextchatUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Some architectural notes:
 * Issued commands will always go through here. It is their own job to find out for which game they have been issued,
 * and make the appropriate calls or handle any user errors
 */
public class CommandHandler {

    private final static Logger log = LoggerFactory.getLogger(CommandHandler.class);

    private static final Map<String, ICommand> COMMAND_REGISTRY = new HashMap<>();

    static {
        //game related commands
        COMMAND_REGISTRY.put(InCommand.COMMAND, new InCommand());
        COMMAND_REGISTRY.put(OutCommand.COMMAND, new OutCommand());
        COMMAND_REGISTRY.put(RolePMCommand.COMMAND, new RolePMCommand());
        COMMAND_REGISTRY.put(SetupCommand.COMMAND, new SetupCommand());
        COMMAND_REGISTRY.put(ShootCommand.COMMAND, new ShootCommand());
        COMMAND_REGISTRY.put(StartCommand.COMMAND, new StartCommand());
        COMMAND_REGISTRY.put(StatusCommand.COMMAND, new StatusCommand());

        //other commands
        COMMAND_REGISTRY.put(HelpCommand.COMMAND, new HelpCommand());
        COMMAND_REGISTRY.put(InfoCommand.COMMAND, new InfoCommand());
        COMMAND_REGISTRY.put(ReplayCommand.COMMAND, new ReplayCommand());
        COMMAND_REGISTRY.put(ChannelSettingsCommand.COMMAND, new ChannelSettingsCommand());
        COMMAND_REGISTRY.put(TagCommand.COMMAND, new TagCommand());
        COMMAND_REGISTRY.put(UserStatsCommand.COMMAND, new UserStatsCommand());
        COMMAND_REGISTRY.put(GuildStatsCommand.COMMAND, new GuildStatsCommand());
        COMMAND_REGISTRY.put(BotStatsCommand.COMMAND, new BotStatsCommand());

        //bot owner/debug commands
        COMMAND_REGISTRY.put(DbTestCommand.COMMAND, new DbTestCommand());
        COMMAND_REGISTRY.put(EvalCommand.COMMAND, new EvalCommand());
        COMMAND_REGISTRY.put(ShutdownCommand.COMMAND, new ShutdownCommand());
        COMMAND_REGISTRY.put(UpdateCommand.COMMAND, new UpdateCommand());
        COMMAND_REGISTRY.put(MaintenanceCommand.COMMAND, new MaintenanceCommand());
        COMMAND_REGISTRY.put(RegisterPrivateServerCommand.COMMAND, new RegisterPrivateServerCommand());
        COMMAND_REGISTRY.put(BanCommand.COMMAND, new BanCommand());
    }

    public static void handleCommand(final CommandParser.CommandContainer commandInfo) {
        try {
            final ICommand command = COMMAND_REGISTRY.get(commandInfo.command);
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
            final boolean success = command.execute(commandInfo);
            final long executed = System.currentTimeMillis();
            Wolfia.executor.submit(() -> DbWrapper.persist(new CommandStats(commandInfo, command.getClass(), executed, success)));
        } catch (final Exception e) {
            final MessageReceivedEvent ev = commandInfo.event;
            Throwable t = e;
            while (t != null) {
                log.error("Exception `{}` while handling a command in guild {}, channel {}, user {}, invite {}",
                        t.getMessage(), ev.getGuild().getIdLong(), ev.getChannel().getIdLong(),
                        ev.getAuthor().getIdLong(), TextchatUtils.getOrCreateInviteLink(ev.getTextChannel()), t);
                t = t.getCause();
            }
            Wolfia.handleOutputMessage(ev.getTextChannel(),
                    "%s, an internal exception happened while executing your command\n`%s`\nSorry about that. Please " +
                            "contact the developer through the website or Discord guild sent to you through `%s`",
                    ev.getAuthor().getAsMention(), commandInfo.raw, Config.PREFIX + HelpCommand.COMMAND);
        }
    }
}
