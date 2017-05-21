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


import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 23.08.2016
 */
public interface ICommand {

    //this is called to check whether the arguments the user provided are ok
    default boolean argumentsValid(final String[] args, final MessageReceivedEvent event) {
        return true;
    }

    //executes the command
    void execute(CommandParser.CommandContainer commandInfo);

    //return a help string that should explain the usage of this command
    String help();
}
