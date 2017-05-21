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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.ICommand;
import space.npstr.wolfia.game.GameSetup;
import space.npstr.wolfia.game.Setups;

/**
 * Created by npstr on 23.08.2016
 */
public class OutCommand implements ICommand {

    public static final String COMMAND = "out";


    public OutCommand() {
    }

    @Override
    public boolean argumentsValid(final String[] args, final MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {
        final GameSetup setup = Setups.get(commandInfo.event.getChannel().getIdLong());
        if (setup != null) {
            setup.outPlayer(commandInfo.event.getAuthor().getIdLong());
        }
    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND + "\nwill remove you from the current signup list```";
    }

}
