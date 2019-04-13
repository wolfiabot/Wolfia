/*
 * Copyright (C) 2017-2019 Dennis Neufeld
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

package space.npstr.wolfia.engine.wincondition

import space.npstr.wolfia.engine.Alignment
import space.npstr.wolfia.engine.Game

class SimpleParity : WinCondition(Alignment.BADDIE) {

    override fun isMet(game: Game): Boolean {
        val livingGoodies = game.players.count { it.alignment == Alignment.GOODIE && it.alive }
        val livingBaddies = game.players.count { it.alignment == Alignment.BADDIE && it.alive }

        return livingBaddies >= livingGoodies
    }
}
