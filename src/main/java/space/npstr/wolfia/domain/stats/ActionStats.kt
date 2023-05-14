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

import space.npstr.wolfia.game.definitions.Actions
import space.npstr.wolfia.game.definitions.Phase

/**
 * Describe an action that happened during a game
 */
data class ActionStats(
	val actionId: Long,
	val actionType: Actions, //userId of the discord user; there might be special negative values for factional actors/targets in the future
	val actor: Long, //n0, d1 + n1, d2 + n2 etc
	val cycle: Int, //chronological order of the actions
	//order is a reserved keyword in postgres, so we use 'sequence' in the table instead
	val order: Int, //userId of the discord user
	val target: Long,
	val timeStampHappened: Long,
	// the difference between these two timestamps is the following: an action may be submitted before it actually
	// happens (example: nk gets submitted during the night, but actually "happens" when the day starts and results are
	// announced). these two timestamps try to capture that data as accurately as possible
	val timeStampSubmitted: Long,
	val phase: Phase,
	//save any additional info of an action in here
	val additionalInfo: String?,
) {
	override fun hashCode(): Int {
		return actionId.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		return other is ActionStats && other.actionId == this.actionId
	}
}
