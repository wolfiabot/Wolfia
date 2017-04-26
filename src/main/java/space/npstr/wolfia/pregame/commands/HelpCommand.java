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

package space.npstr.wolfia.pregame.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.Command;
import space.npstr.wolfia.CommandListener;
import space.npstr.wolfia.Main;

import java.util.Map;

/**
 * Created by npstr on 09.09.2016
 */
public class HelpCommand extends Command {

    public final static String COMMAND = "help";
    private final String HELP = "```usage: " + getListener().getPrefix() + COMMAND + " (<command>)\nto see all available commands " +
            "for this channel or see the help for a specific command```";

    private final Map<String, Command> commands;

    public HelpCommand(CommandListener l, Map<String, Command> commands) {
        super(l);
        this.commands = commands;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        if (args.length > 0) {
            if (commands.get(args[0]) == null)
                return false;
        }
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        String out;
        if (args.length < 1) {
            out = "Available commands in this channel:\n```";
            for (String s : commands.keySet()) out += getListener().getPrefix() + s + ", ";
            if (commands.size() > 0) out = out.substring(0, out.length() - 2);
            out += "```";
        } else {
            out = commands.get(args[0]).help();
        }
        Main.handleOutputMessage(event.getTextChannel(), out);
        return true;
    }

    @Override
    public String help() {
        return HELP;
    }
}
