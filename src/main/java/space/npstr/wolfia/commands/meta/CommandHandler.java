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

import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.*;
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

    private static final Map<String, ICommand> COMMAND_REGISTRY = new HashMap<>();

    static {
//        COMMAND_REGISTRY.put(HelpCommand.COMMAND, new HelpCommand());
        COMMAND_REGISTRY.put(InCommand.COMMAND, new InCommand());
        COMMAND_REGISTRY.put(OutCommand.COMMAND, new OutCommand());
        COMMAND_REGISTRY.put(SetupCommand.COMMAND, new SetupCommand());
        COMMAND_REGISTRY.put(ShootCommand.COMMAND, new ShootCommand());
        COMMAND_REGISTRY.put(StartCommand.COMMAND, new StartCommand());
        COMMAND_REGISTRY.put(StatusCommand.COMMAND, new StatusCommand());
    }

    public static void handleCommand(final CommandContainer commandInfo) {
        final ICommand command = COMMAND_REGISTRY.get(commandInfo.command);
        if (command == null) {
            //TODO decide how to handle unknown command
        } else {
            if (!command.argumentsValid(commandInfo.args, commandInfo.event)) {
                Wolfia.handleOutputMessage(commandInfo.event.getChannel(), command.help());
            } else {
                command.execute(commandInfo);
            }
        }
    }
}
