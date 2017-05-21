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

package space.npstr.wolfia.game;

import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.IGameCommand;
import space.npstr.wolfia.utils.IllegalGameStateException;

import java.util.Set;

/**
 * Created by npstr on 14.09.2016
 */
public abstract class Game {

    public abstract Set<Integer> getAmountOfPlayers();

    public abstract void start(Set<Long> players);

    public abstract boolean isAcceptablePlayerCount(int signedUp);

    public abstract void issueCommand(IGameCommand command, CommandParser.CommandContainer commandInfo) throws IllegalGameStateException;

    /**
     * this should revert each and everything the game touches in terms of discord roles and permissions to normal
     * most likely this includes deleting all discord roles used in the game and resetting @everyone permissions for the game channel
     */
    public abstract void resetRolesAndPermissions();

    /**
     * @return Returns the main channel where the game is running
     */
    public abstract long getChannelId();

    /**
     * @return a status of the game
     */
    public abstract String getStatus();
}
