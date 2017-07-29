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

package space.npstr.wolfia.commands.game;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.game.IllegalGameStateException;

/**
 * Created by npstr on 14.09.2016
 * <p>
 * any signed up player can use this command to start a game
 */
public class StartCommand implements ICommand {

    public final static String COMMAND = "start";

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        final SetupEntity setup = DbWrapper.getOrCreateEntity(commandInfo.event.getChannel().getIdLong(), SetupEntity.class);
        return setup.startGame(commandInfo.event.getAuthor().getIdLong());
    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND
                + " \nto start the game. Game will only start if enough players have signed up\n";
    }
}
