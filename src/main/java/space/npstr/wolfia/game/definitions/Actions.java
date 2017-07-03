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

package space.npstr.wolfia.game.definitions;

/**
 * Created by napster on 02.06.17.
 * <p>
 * Possible actions in the game
 */
public enum Actions {

    //general game stuff
    GAMESTART,
    GAMEEND,
    DAYSTART,
    DAYEND,
    NIGHTSTART,
    NIGHTEND,
    BOTKILL, //auto kill by the bot
    MODKILL, //killed by a game mod (reserved for the future )

    DEATH,

    //popcorn related
    SHOOT,
    VOTEGUN,
    GIVEGUN
}
