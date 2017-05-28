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

import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.ICommand;

/**
 * Created by npstr on 09.09.2016
 */
public class HelpCommand implements ICommand {

    public final static String COMMAND = "help";

    public HelpCommand() {
    }

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {
//        String out;
//        if (commandInfo.args.length < 1) {
//            out = "Available commands in this channel:\n```";
//            for (final String s : this.commands.keySet()) out += Config.PREFIX + s + ", ";
//            if (this.commands.size() > 0) out = out.substring(0, out.length() - 2);
//            out += "```";
//        } else {
//            out = this.commands.get(commandInfo.args[0]).help();
//        }
//        Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(), out);
    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND + " (<command>)\nto see all available commands for this channel "
                + "or see the help for a specific command```";
    }
}
