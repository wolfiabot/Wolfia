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
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Games

/**
 * Describe a game that happened
 */
data class InsertGameStats(
	val guildId: Long,
	val guildName: String,
	val channelId: Long,
	val channelName: String,
	val gameType: Games,
	val gameMode: GameMode,
	val playerSize: Int,
) {
	private val startingTeams = mutableSetOf<InsertTeamStats>()
	private val actions = mutableSetOf<InsertActionStats>()

	val startTime: Long = System.currentTimeMillis()
	var endTime: Long = 0

	fun addAction(action: InsertActionStats) {
		this.actions.add(action)
	}

	fun addActions(actions: Collection<InsertActionStats>) {
		this.actions.addAll(actions)
	}

	fun addTeam(team: InsertTeamStats) {
		startingTeams.add(team)
	}

	fun setTeams(teams: Collection<InsertTeamStats>) {
		startingTeams.clear()
		startingTeams.addAll(teams)
	}

	override fun hashCode(): Int {
		return Objects.hash(startTime, guildId, channelId)
	}

	override fun equals(other: Any?): Boolean {
		return other is InsertGameStats
			&& other.startTime == this.startTime
			&& other.guildId == this.guildId
			&& other.channelId == this.channelId
	}

	fun getStartingTeams(): Set<InsertTeamStats> {
		return Collections.unmodifiableSet(startingTeams)
	}

	fun getActions(): Set<InsertActionStats> {
		return Collections.unmodifiableSet(actions)
	}
}
