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
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.ICommand;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameSetup;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.game.Setups;

/**
 * Created by npstr on 24.08.2016
 * <p>
 * this command should display the status of whatever is happening in a channel currently
 * <p>
 * is there a game running, whats it's state?
 * if not, is there a setup created for this channel, whats the status here, inned players etc?
 * if not, explain how to start a game setup
 */
public class StatusCommand implements ICommand {

    public static final String COMMAND = "status";

    public StatusCommand() {
    }

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {

        final Game game = Games.get(commandInfo.event.getChannel().getIdLong());
        if (game != null) {
            Wolfia.handleOutputMessage(commandInfo.event.getChannel(), game.getStatus());
            return;
        }

        final GameSetup setup = Setups.get(commandInfo.event.getChannel().getIdLong());
        if (setup != null) {
            Wolfia.handleOutputMessage(commandInfo.event.getChannel(), setup.getStatus());
            return;
        }

        Wolfia.handleOutputMessage(commandInfo.event.getChannel(),
                "Nothing going on in here yet! Start setting up a game with `%s%s`.", Config.PREFIX, SetupCommand.COMMAND);
    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND + "\nposts the current game status or sign up list```";
    }
}
