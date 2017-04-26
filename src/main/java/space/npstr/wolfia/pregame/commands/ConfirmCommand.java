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
import space.npstr.wolfia.pregame.Pregame;

/**
 * Created by npstr on 22.10.2016
 */
public class ConfirmCommand extends Command {

    public final static String COMMAND = "confirm";
    private final String HELP = "```usage: " + getListener().getPrefix()
            + COMMAND + " \nto start the game. Game will only start if enough players have signed up\n";

    private Pregame pg;

    public ConfirmCommand(CommandListener listener, Pregame pg) {
        super(listener);
        this.pg = pg;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        pg.confirm(event.getAuthor().getId());
        return false;
    }

    @Override
    public String help() {
        return HELP;
    }
}
