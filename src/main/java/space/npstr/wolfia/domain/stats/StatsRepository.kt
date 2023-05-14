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

import java.math.BigDecimal
import java.time.Instant
import okio.use
import org.jooq.Record3
import org.jooq.Record8
import org.jooq.RecordMapper
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.Database
import space.npstr.wolfia.db.gen.Tables
import space.npstr.wolfia.db.gen.tables.records.StatsActionRecord
import space.npstr.wolfia.db.gen.tables.records.StatsPlayerRecord
import space.npstr.wolfia.db.gen.tables.records.StatsTeamRecord
import space.npstr.wolfia.domain.privacy.PrivacyAction
import space.npstr.wolfia.domain.privacy.PrivacyGame
import space.npstr.wolfia.game.GameInfo
import space.npstr.wolfia.game.definitions.Actions
import space.npstr.wolfia.game.definitions.Alignments
import space.npstr.wolfia.game.definitions.Games
import space.npstr.wolfia.game.definitions.Phase
import space.npstr.wolfia.system.metrics.MetricsRegistry

@Repository
internal class StatsRepository(
	private val database: Database,
) {

	fun getAveragePlayerSize(): BigDecimal {
		return MetricsRegistry.queryTime.labels("getAveragePlayerSize").startTimer().use {
			database.jooq()
				.select(DSL.avg(Tables.STATS_GAME.PLAYER_SIZE))
				.from(Tables.STATS_GAME)
				.fetchOne()?.component1() ?: BigDecimal.ZERO // SQL AVG may return null for empty sets
		}
	}

	fun getAveragePlayerSizeInGuild(guildId: Long): BigDecimal {
		return MetricsRegistry.queryTime.labels("getAveragePlayerSizeInGuild").startTimer().use {
			database.jooq()
				.select(DSL.avg(Tables.STATS_GAME.PLAYER_SIZE))
				.from(Tables.STATS_GAME)
				.where(Tables.STATS_GAME.GUILD_ID.eq(guildId))
				.fetchOne()?.component1() ?: BigDecimal.ZERO // SQL AVG may return null for empty sets
		}
	}

	fun getDistinctPlayerSizes(): Set<Int> {
		return MetricsRegistry.queryTime.labels("getDistinctPlayerSizes").startTimer().use {
			database.jooq()
				.selectDistinct(Tables.STATS_GAME.PLAYER_SIZE)
				.from(Tables.STATS_GAME)
				.fetch()
				.intoSet(Tables.STATS_GAME.PLAYER_SIZE)
		}
	}

	fun getDistinctPlayerSizesInGuild(guildId: Long): Set<Int> {
		return MetricsRegistry.queryTime.labels("getDistinctPlayerSizesInGuild").startTimer().use {
			database.jooq()
				.selectDistinct(Tables.STATS_GAME.PLAYER_SIZE)
				.from(Tables.STATS_GAME)
				.where(Tables.STATS_GAME.GUILD_ID.eq(guildId))
				.fetch()
				.intoSet(Tables.STATS_GAME.PLAYER_SIZE)
		}
	}

	fun countAlignmentWins(alignment: Alignments): Int {
		return MetricsRegistry.queryTime.labels("countAlignmentWins").startTimer().use {
			database.jooq()
				.select(DSL.count())
				.from(Tables.STATS_GAME)
				.innerJoin(Tables.STATS_TEAM)
				.on(Tables.STATS_TEAM.GAME_ID.eq(Tables.STATS_GAME.GAME_ID))
				.where(Tables.STATS_TEAM.IS_WINNER.isTrue)
				.and(Tables.STATS_TEAM.ALIGNMENT.eq(alignment.name))
				.fetchSingle()
				.component1()

		}
	}

	fun countAlignmentWinsInGuild(alignment: Alignments, guildId: Long): Int {
		return MetricsRegistry.queryTime.labels("countAlignmentWinsInGuild").startTimer().use {
			database.jooq()
				.select(DSL.count())
				.from(Tables.STATS_GAME)
				.innerJoin(Tables.STATS_TEAM)
				.on(Tables.STATS_TEAM.GAME_ID.eq(Tables.STATS_GAME.GAME_ID))
				.where(Tables.STATS_TEAM.IS_WINNER.isTrue)
				.and(Tables.STATS_TEAM.ALIGNMENT.eq(alignment.name))
				.and(Tables.STATS_GAME.GUILD_ID.eq(guildId))
				.fetchSingle()
				.component1()
		}
	}

	fun countAlignmentWinsForPlayerSize(alignment: Alignments, playerSize: Int): Int {
		return MetricsRegistry.queryTime.labels("countAlignmentWinsForPlayerSize").startTimer().use {
			database.jooq()
				.select(DSL.count())
				.from(Tables.STATS_GAME)
				.innerJoin(Tables.STATS_TEAM)
				.on(Tables.STATS_TEAM.GAME_ID.eq(Tables.STATS_GAME.GAME_ID))
				.where(Tables.STATS_TEAM.IS_WINNER.isTrue)
				.and(Tables.STATS_GAME.PLAYER_SIZE.eq(playerSize))
				.and(Tables.STATS_TEAM.ALIGNMENT.eq(alignment.name))
				.fetchSingle()
				.component1()
		}
	}

	fun countAlignmentWinsForPlayerSizeInGuild(alignment: Alignments, playerSize: Int, guildId: Long): Int {
		return MetricsRegistry.queryTime.labels("countAlignmentWinsForPlayerSizeInGuild").startTimer().use {
			database.jooq()
				.select(DSL.count())
				.from(Tables.STATS_GAME)
				.innerJoin(Tables.STATS_TEAM)
				.on(Tables.STATS_TEAM.GAME_ID.eq(Tables.STATS_GAME.GAME_ID))
				.where(Tables.STATS_TEAM.IS_WINNER.isTrue)
				.and(Tables.STATS_GAME.PLAYER_SIZE.eq(playerSize))
				.and(Tables.STATS_TEAM.ALIGNMENT.eq(alignment.name))
				.and(Tables.STATS_GAME.GUILD_ID.eq(guildId))
				.fetchSingle()
				.component1()
		}
	}

	fun getGeneralUserStats(userId: Long): List<GeneralUserStats> {
		return MetricsRegistry.queryTime.labels("getGeneralUserStats").startTimer().use {
			database.jooq()
				.select(Tables.STATS_PLAYER.TOTAL_POSTLENGTH, Tables.STATS_PLAYER.TOTAL_POSTS, Tables.STATS_PLAYER.ALIGNMENT, Tables.STATS_TEAM.IS_WINNER)
				.from(Tables.STATS_PLAYER)
				.innerJoin(Tables.STATS_TEAM).on(Tables.STATS_PLAYER.TEAM_ID.eq(Tables.STATS_TEAM.TEAM_ID))
				.where(Tables.STATS_PLAYER.USER_ID.eq(userId))
				.fetchInto(GeneralUserStats::class.java)
		}
	}

	fun getUserShots(userId: Long): List<String> {
		return MetricsRegistry.queryTime.labels("getUserShots").startTimer().use {
			database.jooq()
				.select(Tables.STATS_PLAYER.ALIGNMENT)
				.from(Tables.STATS_ACTION)
				.innerJoin(Tables.STATS_PLAYER).on(Tables.STATS_PLAYER.USER_ID.eq(Tables.STATS_ACTION.TARGET))
				.innerJoin(Tables.STATS_TEAM).on(Tables.STATS_TEAM.TEAM_ID.eq(Tables.STATS_PLAYER.TEAM_ID))
				.innerJoin(Tables.STATS_GAME)
				.on(Tables.STATS_ACTION.GAME_ID.eq(Tables.STATS_GAME.GAME_ID).and(Tables.STATS_TEAM.GAME_ID.eq(Tables.STATS_GAME.GAME_ID)))
				.where(Tables.STATS_ACTION.ACTION_TYPE.eq("SHOOT").and(Tables.STATS_ACTION.ACTOR.eq(userId)))
				.fetch()
				.intoArray(Tables.STATS_PLAYER.ALIGNMENT)
				.toList()
		}
	}

	fun findGameStats(gameId: Long): GameStats? {
		return MetricsRegistry.queryTime.labels("findGameStats").startTimer().use {
			val dsl = database.jooq()
			val gameRecord = dsl.selectFrom(Tables.STATS_GAME)
				.where(Tables.STATS_GAME.GAME_ID.eq(gameId))
				.fetchOne()
				?: return@use null

			val teams = dsl.selectFrom(Tables.STATS_TEAM)
				.where(Tables.STATS_TEAM.GAME_ID.eq(gameId))
				.fetch()
				.map { teamRecord ->
					val players = dsl.selectFrom(Tables.STATS_PLAYER)
						.where(Tables.STATS_PLAYER.TEAM_ID.eq(teamRecord.teamId))
						.fetch(playerMapper())
					return@map mapTeam(teamRecord, players)
				}

			val actions = dsl.selectFrom(Tables.STATS_ACTION)
				.where(Tables.STATS_ACTION.GAME_ID.eq(gameId))
				.fetch(actionMapper())

			return@use GameStats(
				gameRecord.gameId,
				gameRecord.channelId,
				gameRecord.channelName,
				gameRecord.endTime,
				GameInfo.GameMode.valueOf(gameRecord.gameMode),
				Games.valueOf(gameRecord.gameType),
				gameRecord.guildId,
				gameRecord.guildName,
				gameRecord.startTime,
				gameRecord.playerSize,
				teams,
				actions,
			)
		}
	}

	private fun mapTeam(teamRecord: StatsTeamRecord, players: Collection<PlayerStats>): TeamStats {
		return TeamStats(
			teamRecord.teamId, Alignments.valueOf(teamRecord.alignment), teamRecord.isWinner,
			teamRecord.name, teamRecord.teamSize, players,
		)
	}

	private fun playerMapper(): RecordMapper<StatsPlayerRecord, PlayerStats> {
		return RecordMapper { record: StatsPlayerRecord ->
			PlayerStats(
				record.playerId, record.nickname, record.role,
				record.totalPostlength, record.totalPosts, record.userId, Alignments.valueOf(record.alignment),
			)
		}
	}

	private fun actionMapper(): RecordMapper<StatsActionRecord, ActionStats> {
		return RecordMapper { record: StatsActionRecord ->
			ActionStats(
				record.actionId, Actions.valueOf(record.actionType), record.actor,
				record.cycle, record.sequence, record.target, record.happened,
				record.submitted, Phase.valueOf(record.phase), record.additionalInfo,
			)
		}
	}

	fun insertGameStats(insertGameStats: InsertGameStats): GameStats {
		return MetricsRegistry.queryTime.labels("insertGameStats").startTimer().use {
			database.jooq().transactionResult { config ->
				val context = config.dsl()
				val gameId = context
					.insertInto(Tables.STATS_GAME)
					.values(
						DSL.defaultValue(Tables.STATS_GAME.GAME_ID), insertGameStats.channelId,
						insertGameStats.channelName, insertGameStats.endTime, insertGameStats.gameMode.name,
						insertGameStats.gameType.name, insertGameStats.guildId, insertGameStats.guildName,
						insertGameStats.startTime, insertGameStats.playerSize,
					)
					.returningResult(Tables.STATS_GAME.GAME_ID)
					.fetchSingle()
					.component1()


				val teams = insertGameStats.startingTeams.map mapTeam@{ teamStats ->
					val teamRecord = context
						.insertInto(Tables.STATS_TEAM)
						.values(
							DSL.defaultValue(Tables.STATS_TEAM.TEAM_ID), teamStats.alignment,
							teamStats.isWinner, teamStats.name, gameId, teamStats.teamSize,
						)
						.returning()
						.fetchSingle()
					val players = teamStats.players.map mapPlayer@{ playerStats ->
						return@mapPlayer context
							.insertInto(Tables.STATS_PLAYER)
							.values(
								DSL.defaultValue(Tables.STATS_PLAYER.PLAYER_ID), playerStats.nickname,
								playerStats.role.name, playerStats.totalPostLength,
								playerStats.totalPosts, playerStats.userId, teamRecord.teamId,
								playerStats.alignment.name,
							)
							.returning()
							.fetchSingle(playerMapper())

					}
					return@mapTeam mapTeam(teamRecord, players)
				}

				val actions = insertGameStats.actions.map { actionStats ->
					return@map context
						.insertInto(Tables.STATS_ACTION)
						.values(
							DSL.defaultValue(Tables.STATS_ACTION.ACTION_ID), actionStats.actionType.name,
							actionStats.actor, actionStats.cycle, actionStats.order,
							actionStats.target, actionStats.timeStampHappened,
							actionStats.timeStampSubmitted, gameId, actionStats.phase.name,
							actionStats.additionalInfo,
						)
						.returning()
						.fetchSingle(actionMapper())
				}

				return@transactionResult GameStats(
					gameId, insertGameStats.channelId, insertGameStats.channelName,
					insertGameStats.endTime, insertGameStats.gameMode, insertGameStats.gameType,
					insertGameStats.guildId, insertGameStats.guildName, insertGameStats.startTime,
					insertGameStats.playerSize, teams, actions,
				)
			}

		}
	}

	fun getAllGameStatsOfUser(userId: Long): List<PrivacyGame> {
		return MetricsRegistry.queryTime.labels("getAllGameStatsOfUser").startTimer().use {
			database.jooq()
				.select(
					Tables.STATS_GAME.GAME_ID,
					Tables.STATS_GAME.START_TIME,
					Tables.STATS_GAME.END_TIME,
					Tables.STATS_TEAM.ALIGNMENT,
					Tables.STATS_TEAM.IS_WINNER,
					Tables.STATS_PLAYER.NICKNAME,
					Tables.STATS_PLAYER.TOTAL_POSTS,
					Tables.STATS_PLAYER.TOTAL_POSTLENGTH,
				)
				.from(Tables.STATS_GAME)
				.join(Tables.STATS_TEAM).on(Tables.STATS_TEAM.GAME_ID.eq(Tables.STATS_GAME.GAME_ID))
				.join(Tables.STATS_PLAYER).on(Tables.STATS_PLAYER.TEAM_ID.eq(Tables.STATS_TEAM.TEAM_ID))
				.where(Tables.STATS_PLAYER.USER_ID.eq(userId))
				.fetch(privacyGameMapper())
		}
	}

	private fun privacyGameMapper(): RecordMapper<Record8<Long, Long, Long, String, Boolean, String, Int, Int>, PrivacyGame> {
		return RecordMapper { record ->
			PrivacyGame(
				record.get(Tables.STATS_GAME.GAME_ID),
				Instant.ofEpochMilli(record.get(Tables.STATS_GAME.START_TIME)),
				Instant.ofEpochMilli(record.get(Tables.STATS_GAME.END_TIME)),
				record.get(Tables.STATS_TEAM.ALIGNMENT),
				record.get(Tables.STATS_TEAM.IS_WINNER),
				record.get(Tables.STATS_PLAYER.NICKNAME),
				record.get(Tables.STATS_PLAYER.TOTAL_POSTS),
				record.get(Tables.STATS_PLAYER.TOTAL_POSTLENGTH), listOf(),
			)
		}
	}

	fun getAllActionStatsOfUser(userId: Long): Map<Long, List<PrivacyAction>> {
		return MetricsRegistry.queryTime.labels("getAllActionStatsOfUser").startTimer().use {
			database.jooq()
				.select(
					Tables.STATS_GAME.GAME_ID,
					Tables.STATS_ACTION.ACTION_TYPE,
					Tables.STATS_ACTION.SUBMITTED,
				)
				.from(Tables.STATS_GAME)
				.join(Tables.STATS_ACTION).on(Tables.STATS_ACTION.GAME_ID.eq(Tables.STATS_GAME.GAME_ID))
				.where(Tables.STATS_ACTION.ACTOR.eq(userId))
				.fetchGroups(Tables.STATS_GAME.GAME_ID, privacyActionMapper())
		}
	}

	private fun privacyActionMapper(): RecordMapper<Record3<Long, String, Long>, PrivacyAction> {
		return RecordMapper { record ->
			PrivacyAction(
				Actions.valueOf(record.get(Tables.STATS_ACTION.ACTION_TYPE)),
				Instant.ofEpochMilli(record.get(Tables.STATS_ACTION.SUBMITTED))
			)
		}
	}

	fun nullAllPlayerNicknamesofUser(userId: Long): Int {
		return MetricsRegistry.queryTime.labels("nullAllPlayerNicknamesofUser").startTimer().use {
			database.jooq().transactionResult { config ->
				config.dsl()
					.update(Tables.STATS_PLAYER)
					.set(Tables.STATS_PLAYER.NICKNAME, DSL.`val`(null, Tables.STATS_PLAYER.NICKNAME))
					.where(Tables.STATS_PLAYER.USER_ID.eq(userId))
					.execute()
			}
		}
	}
}
