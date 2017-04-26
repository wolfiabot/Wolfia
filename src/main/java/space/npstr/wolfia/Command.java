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

package space.npstr.wolfia;


import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 23.08.2016
 */
public abstract class Command {

    //TODO: implement getCommand to retrieve the command string just like the help string

    private final CommandListener listener;

    public Command(CommandListener listener) {
        this.listener = listener;
    }

    public CommandListener getListener() {
        return listener;
    }

    //this is called to check whether the arguments the user provided are ok
    public abstract boolean argumentsValid(String[] args, MessageReceivedEvent event);

    //executes the command
    public abstract boolean execute(String[] args, MessageReceivedEvent event);

    //return a help string that should explain the usage of this command
    public abstract String help();

    //this handles output after the execution
    public void executed(boolean success, MessageReceivedEvent event) {
        if (!success) {
            Main.handleOutputMessage(event.getTextChannel(), help());
        }
    }
}
