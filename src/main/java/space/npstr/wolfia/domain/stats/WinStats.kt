/*
 * Copyright (C) 2016-2023 the original author or authors
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
package space.npstr.wolfia.domain.stats

data class WinStats(
	/**
	 * @return the player size that these win stats belong to
	 */
	val playerSize: Int,
	/**
	 * @return total games with this player size
	 */
	val totalGames: Int,
	/**
	 * @return goodie wins with this player size
	 */
	val goodieWins: Int,
	/**
	 * @return baddie wins with this player size
	 */
	val baddieWins: Int,
)
