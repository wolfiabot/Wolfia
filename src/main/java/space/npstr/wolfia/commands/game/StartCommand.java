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

import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.db.entities.SetupEntity;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

/**
 * Created by npstr on 14.09.2016
 * <p>
 * any signed up player can use this command to start a game
 */
public class StartCommand extends BaseCommand {

    public StartCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger()
                + "\n#Start the game. Game will only start if enough players have signed up.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo)
            throws IllegalGameStateException, DatabaseException {
        final SetupEntity setup = Wolfia.getDbWrapper().getOrCreate(commandInfo.event.getChannel().getIdLong(), SetupEntity.class);
        return setup.startGame(commandInfo.event.getAuthor().getIdLong());
    }
}
