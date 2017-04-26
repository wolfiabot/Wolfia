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

import net.dv8tion.jda.core.entities.Channel;

import java.util.Map;
import java.util.Set;

/**
 * Created by npstr on 14.09.2016
 */
public interface Game {

    public int getAmountOfPlayers();

    public void start(Set<String> players);

    public boolean enoughPlayers(int signedUp);

    public Map<String, Command> getGameCommands();

    /**
     * this should revert each and everything the game touches in terms of discord roles and permissions to normal
     * most likely this includes deleting all discord roles used in the game and resetting @everyone permissions for the game channel
     */
    public void resetRolesAndPermissions();

    /**
     * @return Returns the main channel where the game is running
     */
    public Channel getChannel();
}
