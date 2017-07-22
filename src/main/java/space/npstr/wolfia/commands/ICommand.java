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


import space.npstr.wolfia.game.IllegalGameStateException;

/**
 * Created by npstr on 23.08.2016
 */
public interface ICommand {

    //executes the command
    boolean execute(CommandParser.CommandContainer commandInfo) throws IllegalGameStateException;

    //return a help string that should explain the usage of this command
    String help();
}
