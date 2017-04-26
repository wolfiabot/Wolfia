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

package space.npstr.wolfia.PopcornGame.Commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.Command;
import space.npstr.wolfia.CommandListener;

/**
 * Created by npstr on 09.11.2016
 */
public class GiveGunCommand extends Command {

    public final static String COMMAND = "gun";
    private final String HELP = "```usage: " + getListener().getPrefix()
            + COMMAND + " <player>\nto give <player> the gun. This is not a voting, this happens immediately, so " +
            "remember to consult your teams opinion first.```";

    public GiveGunCommand(CommandListener listener) {
        super(listener);
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return false;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        return false;
    }

    @Override
    public String help() {
        return HELP;
    }
}
