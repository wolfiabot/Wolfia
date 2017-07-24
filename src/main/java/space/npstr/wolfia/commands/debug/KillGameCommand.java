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

package space.npstr.wolfia.commands.debug;

import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.IllegalGameStateException;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.UserFriendlyException;

import java.util.Arrays;

/**
 * Created by napster on 24.07.17.
 */
public class KillGameCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "killgame";

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {

        if (commandInfo.args.length < 1) {
            commandInfo.reply("Please provide the channelId of the game you want to kill.");
            return false;
        }

        long channelId = -1;
        try {
            channelId = Long.valueOf(commandInfo.args[0]);
        } catch (final NumberFormatException e) {
            commandInfo.reply("Invalid channelId provided (not a long)");
            return false;
        }

        final Game game = Games.get(channelId);
        if (game == null) {
            commandInfo.reply("There is not game registered for channel " + channelId);
            return false;
        }

        String reason = String.join(" ", Arrays.copyOfRange(commandInfo.args, 1, commandInfo.args.length)).trim();
        if (reason.isEmpty()) reason = "Game killed by bot owner.";
        game.destroy(new UserFriendlyException(reason));

        commandInfo.reply("Game in channel " + channelId + " destroyed.");
        return true;
    }

    @Override
    public String help() {
        return "todo"; //todo
    }
}
