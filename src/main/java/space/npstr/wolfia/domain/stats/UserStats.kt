/*
 * Copyright (C) 2016-2020 the original author or authors
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

import org.immutables.value.Value

@Value.Immutable
@StatsStyle
interface UserStats {
	/**
	 * @return id of the user these stats belong to
	 */
	fun userId(): Long

	/**
	 * @return total amount of games this user participated in
	 */
	fun totalGames(): Long

	/**
	 * @return amount of games this user won
	 */
	fun gamesWon(): Long

	/**
	 * @return amount of games this user played as a baddie
	 */
	fun gamesAsBaddie(): Long

	/**
	 * @return amount of games this user played as a baddie and won
	 */
	fun gamesWonAsBaddie(): Long

	/**
	 * @return amount of games this user played as a goodie
	 */
	fun gamesAsGoodie(): Long

	/**
	 * @return amount of games this user played as a goodie and won
	 */
	fun gamesWonAsGoodie(): Long

	/**
	 * @return total shots by this player
	 */
	fun totalShots(): Long

	/**
	 * @return amount of wolves shot by this user
	 */
	fun wolvesShot(): Long

	/**
	 * @return total posts written by this user
	 */
	fun totalPosts(): Long

	/**
	 * @return total length of posts written by this user
	 */
	fun totalPostLength(): Long
}
