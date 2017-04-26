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
 * Created by npstr on 24.08.2016
 */
public class SignUpStatusCommand extends Command {

    public static final String COMMAND = "signups";
    private final String HELP = "```usage: " + getListener().getPrefix() + COMMAND + "\nposts the current signup list```";

    private final Pregame pg;

    public SignUpStatusCommand(CommandListener l, Pregame pg) {
        super(l);
        this.pg = pg;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        pg.postSignUps();
        return true;
    }

    @Override
    public String help() {
        return HELP;
    }
}
