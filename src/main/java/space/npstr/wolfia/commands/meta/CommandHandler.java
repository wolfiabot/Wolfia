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

package space.npstr.wolfia.commands.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.*;
import space.npstr.wolfia.commands.debug.EvalCommand;
import space.npstr.wolfia.commands.debug.StatsCommand;
import space.npstr.wolfia.commands.debug.UpdateCommand;
import space.npstr.wolfia.commands.meta.CommandParser.CommandContainer;

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
//        COMMAND_REGISTRY.put(HelpCommand.COMMAND, new HelpCommand());
        COMMAND_REGISTRY.put(InCommand.COMMAND, new InCommand());
        COMMAND_REGISTRY.put(OutCommand.COMMAND, new OutCommand());
        COMMAND_REGISTRY.put(SetupCommand.COMMAND, new SetupCommand());
        COMMAND_REGISTRY.put(ShootCommand.COMMAND, new ShootCommand());
        COMMAND_REGISTRY.put(StartCommand.COMMAND, new StartCommand());
        COMMAND_REGISTRY.put(StatusCommand.COMMAND, new StatusCommand());
        COMMAND_REGISTRY.put(RolePMCommand.COMMAND, new RolePMCommand());
        COMMAND_REGISTRY.put(EvalCommand.COMMAND, new EvalCommand());
        COMMAND_REGISTRY.put(UpdateCommand.COMMAND, new UpdateCommand());
        COMMAND_REGISTRY.put(StatsCommand.COMMAND, new StatsCommand());
    }

    public static void handleCommand(final CommandContainer commandInfo) {
        final ICommand command = COMMAND_REGISTRY.get(commandInfo.command);
        if (command == null) {
            //unknown command
            log.info("user {} channel {} unknown command issued: {}",
                    commandInfo.event.getAuthor().getIdLong(),
                    commandInfo.event.getChannel().getIdLong(),
                    commandInfo.raw);
            return;
        }

        if (command instanceof IOwnerRestricted && commandInfo.event.getAuthor().getIdLong() != Config.C.ownerId) {
            //not the bot owner
            log.info("user {} channel {} attempted issuing owner restricted command: {}",
                    commandInfo.event.getAuthor().getIdLong(),
                    commandInfo.event.getChannel().getIdLong(),
                    commandInfo.raw);
            return;
        }
        command.execute(commandInfo);

    }
}
