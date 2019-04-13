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

import space.npstr.wolfia.engine.BaseTest
import space.npstr.wolfia.engine.Game
import space.npstr.wolfia.engine.Player
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimpleParityTest : BaseTest() {

    @Test
    fun `2 goodies + 1 baddie = win condition not met`() {
        val players: List<Player> = listOf(
                goodie(),
                goodie(),
                baddie()
        )

        val game = Game(players, emptyList())

        val simpleParity = SimpleParity()
        assertFalse { simpleParity.isMet(game) }
    }

    @Test
    fun `2 goodies + 2 baddies = win condition met`() {
        val players: List<Player> = listOf(
                goodie(),
                goodie(),
                baddie(),
                baddie()
        )

        val game = Game(players, emptyList())

        val simpleParity = SimpleParity()
        assertTrue { simpleParity.isMet(game) }
    }
}
