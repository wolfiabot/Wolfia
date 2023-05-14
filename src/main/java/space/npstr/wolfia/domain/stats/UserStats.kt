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

data class UserStats(
	/**
	 * @return id of the user these stats belong to
	 */
	val userId: Long,
	/**
	 * @return total amount of games this user participated in
	 */
	val totalGames: Int,
	/**
	 * @return amount of games this user won
	 */
	val gamesWon: Int,
	/**
	 * @return amount of games this user played as a baddie
	 */
	val gamesAsBaddie: Int,
	/**
	 * @return amount of games this user played as a baddie and won
	 */
	val gamesWonAsBaddie: Int,
	/**
	 * @return amount of games this user played as a goodie
	 */
	val gamesAsGoodie: Int,
	/**
	 * @return amount of games this user played as a goodie and won
	 */
	val gamesWonAsGoodie: Int,
	/**
	 * @return total shots by this player
	 */
	val totalShots: Int,
	/**
	 * @return amount of wolves shot by this user
	 */
	val wolvesShot: Int,
	/**
	 * @return total posts written by this user
	 */
	val totalPosts: Int,
	/**
	 * @return total length of posts written by this user
	 */
	val totalPostLength: Int,
)
