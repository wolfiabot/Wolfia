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

import org.springframework.stereotype.Component
import space.npstr.wolfia.game.definitions.Alignments
import space.npstr.wolfia.system.logger

/**
 * Collect various stats from the stats tables.
 */
@Component
class StatsProvider(
	private val repository: StatsRepository,
) {

	fun calculateBotStats(): BotStats {
		val averagePlayerSize = repository.fetchAveragePlayerSize()
		var baddieWins = repository.countAlignmentWins(Alignments.WOLF)
		var goodieWins = repository.countAlignmentWins(Alignments.VILLAGE)
		var totalGames = baddieWins + goodieWins // correct for now, may change in the future
		val totalWinStats = WinStats(-1, totalGames, goodieWins, baddieWins)
		val playerSizes = repository.fetchDistinctPlayerSizes()
		val winStats = playerSizes.mapNotNull { playerSize ->
			if (playerSize < 1) {
				//skip and log about weird player sizes in the db
				logger().error("Found unexpected player size {} in the database", playerSize)
				return@mapNotNull null
			}
			baddieWins = repository.countAlignmentWinsForPlayerSize(Alignments.WOLF, playerSize)
			goodieWins = repository.countAlignmentWinsForPlayerSize(Alignments.VILLAGE, playerSize)
			totalGames = baddieWins + goodieWins
			return@mapNotNull WinStats(playerSize, totalGames, goodieWins, baddieWins)
		}
		return BotStats(averagePlayerSize, totalWinStats, winStats)
	}

	fun calculateGuildStats(guildId: Long): GuildStats {
		val averagePlayerSize = repository.fetchAveragePlayerSizeInGuild(guildId)
		var baddieWins = repository.countAlignmentWinsInGuild(Alignments.WOLF, guildId)
		var goodieWins = repository.countAlignmentWinsInGuild(Alignments.VILLAGE, guildId)
		var totalGames = baddieWins + goodieWins
		val totalWinStats = WinStats(-1, totalGames, goodieWins, baddieWins)
		val playerSizes = repository.fetchDistinctPlayerSizesInGuild(guildId)
		val winStats = playerSizes.mapNotNull { playerSize ->
			if (playerSize < 1) {
				//skip and log about weird player sizes in the db
				logger().error("Found unexpected player size {} in the database", playerSize)
				return@mapNotNull null
			}
			baddieWins = repository.countAlignmentWinsForPlayerSizeInGuild(Alignments.WOLF, playerSize, guildId)
			goodieWins = repository.countAlignmentWinsForPlayerSizeInGuild(Alignments.VILLAGE, playerSize, guildId)
			totalGames = baddieWins + goodieWins
			return@mapNotNull WinStats(playerSize, totalGames, goodieWins, baddieWins)
		}
		return GuildStats(guildId, averagePlayerSize, totalWinStats, winStats)
	}

	fun calculateUserStats(userId: Long): UserStats {
		val games = repository.fetchGeneralUserStats(userId)
		val shots = repository.fetchUserShots(userId)
		val totalGamesByUser = games.size
		val gamesWon = games.count(GeneralUserStats::isWinner)
		val gamesAsWolf = games.count { it.alignment == Alignments.WOLF }
		val gamesAsVillage = games.count { it.alignment == Alignments.VILLAGE }
		val gamesWonAsWolf = games
			.filter { it.alignment == Alignments.WOLF }
			.count(GeneralUserStats::isWinner)
		val gamesWonAsVillage = games
			.filter { it.alignment == Alignments.VILLAGE }
			.count(GeneralUserStats::isWinner)
		val totalPostsWritten = games.map(GeneralUserStats::posts).sum()
		val totalPostsLength = games.map(GeneralUserStats::postLength).sum()
		val totalShatsByUser = shots.size
		val wolvesShatted = shots.count { alignment -> alignment == Alignments.WOLF }
		return UserStats(
			userId,
			totalGamesByUser,
			gamesWon,
			gamesAsWolf,
			gamesWonAsWolf,
			gamesAsVillage,
			gamesWonAsVillage,
			totalShatsByUser,
			wolvesShatted,
			totalPostsWritten,
			totalPostsLength,
		)
	}

	fun calculateGameStats(gameId: Long): GameStats? {
		return repository.findGameStats(gameId)
	}
}
