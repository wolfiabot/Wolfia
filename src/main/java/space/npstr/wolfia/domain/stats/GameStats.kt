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

import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Games

/**
 * Describe a game that happened
 */
data class GameStats(
	//this is pretty much an auto incremented id generator starting by 1 and going 1 upwards
	//there are no hard guarantees that there wont be any gaps, or that they will be in any order in the table
	//that's good enough for our use case though (giving games an "easy" to remember number to request replays and stats
	//later, and passively showing off how many games the bot has done)
	val gameId: Long,
	val channelId: Long,
	val channelName: String,
	val endTime: Long,
	val gameMode: GameMode,
	val gameType: Games,
	val guildId: Long,
	val guildName: String,
	val startTime: Long,
	val playerSize: Int,
	val startingTeams: Set<TeamStats>,
	val actions: Set<ActionStats>,
) {
	override fun hashCode(): Int {
		return gameId.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		return other is GameStats && other.gameId == this.gameId
	}
}
