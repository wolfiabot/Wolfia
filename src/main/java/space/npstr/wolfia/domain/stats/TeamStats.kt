/*
 * Copyright (C) 2016-2025 the original author or authors
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

import space.npstr.wolfia.game.definitions.Alignments

/**
 * Model of a team in a game.
 */
data class TeamStats(
	val teamId: Long,
	val alignment: Alignments,
	val isWinner: Boolean,
	val name: String,
	val teamSize: Int,
	val players: Set<PlayerStats>,
) {
	override fun hashCode(): Int {
		return teamId.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		return other is TeamStats && other.teamId == this.teamId
	}
}
