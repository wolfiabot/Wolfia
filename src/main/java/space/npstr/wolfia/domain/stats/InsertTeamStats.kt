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

import java.util.*
import space.npstr.wolfia.game.definitions.Alignments

data class InsertTeamStats(
	val alignment: Alignments,
	// teams of the same alignment (example: wolves) should have unique names
	val name: String,
	var teamSize: Int,
) {

	private val players = mutableSetOf<InsertPlayerStats>()

	var isWinner = false

	fun addPlayer(player: InsertPlayerStats) {
		players.add(player)
	}

	fun setPlayers(players: Collection<InsertPlayerStats>) {
		this.players.clear()
		this.players.addAll(players)
	}

	override fun hashCode(): Int {
		return Objects.hash(alignment, name)
	}

	override fun equals(other: Any?): Boolean {
		return other is InsertTeamStats
			&& other.alignment == this.alignment
			&& other.name == this.name
	}

	fun getPlayers(): Set<InsertPlayerStats> {
		return Collections.unmodifiableSet(players)
	}
}
