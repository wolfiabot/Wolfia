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

import kotlin.jvm.optionals.getOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import org.springframework.stereotype.Component
import space.npstr.wolfia.commands.Context
import space.npstr.wolfia.commands.MessageContext
import space.npstr.wolfia.domain.UserCache
import space.npstr.wolfia.domain.settings.GuildSettingsService
import space.npstr.wolfia.game.Player
import space.npstr.wolfia.game.definitions.Actions
import space.npstr.wolfia.game.definitions.Alignments
import space.npstr.wolfia.game.definitions.Games
import space.npstr.wolfia.game.definitions.Item
import space.npstr.wolfia.game.tools.NiceEmbedBuilder
import space.npstr.wolfia.game.tools.NiceEmbedBuilder.ChunkingField
import space.npstr.wolfia.system.logger
import space.npstr.wolfia.utils.discord.Emojis
import space.npstr.wolfia.utils.discord.TextchatUtils

@Component
class StatsRender(
	private val guildSettingsService: GuildSettingsService,
	private val userCache: UserCache,
) {

	companion object {
		private const val MAX_FIELDS = 25
	}

	fun renderBotStats(context: Context, botstats: BotStats): List<MessageEmbed> {
		val resultEmbeds = mutableListOf<MessageEmbed>()

		val eb = MessageContext.getDefaultEmbedBuilder()
		eb.setTitle("Wolfia stats:")
		context.jda.shardManager?.shardCache?.firstOrNull()?.selfUser?.avatarUrl?.let { eb.setThumbnail(it) }

		// stats for all games
		eb.addBlankField(false)
		val totalGames = botstats.totalWinStats.totalGames
		eb.addField("Total games played", "$totalGames", true)
		eb.addField("∅ player size", String.format("%.2f", botstats.averagePlayerSize.toDouble()), true)
		val baddieWinPercentage = TextchatUtils.divide(botstats.totalWinStats.baddieWins, totalGames)
		val goodieWinPercentage = TextchatUtils.divide(botstats.totalWinStats.goodieWins, totalGames)
		eb.addField("Win % for ${Emojis.WOLF}", TextchatUtils.percentFormat(baddieWinPercentage), true)
		eb.addField("Win % for ${Emojis.COWBOY}", TextchatUtils.percentFormat(goodieWinPercentage), true)

		// stats by playersize
		eb.addBlankField(false)
		eb.addField("Stats by player size:", "", false)

		var sortedWinStats: List<WinStats> = ArrayList(botstats.winStatsByPlayerSize)
			.sortedWith(Comparator.comparingInt(WinStats::playerSize))

		val takeFields = MAX_FIELDS - eb.fields.count()

		addStatsPerPlayerSizeField(eb, sortedWinStats.take(takeFields))
		resultEmbeds.add(eb.build())

		sortedWinStats = sortedWinStats.drop(takeFields)
		sortedWinStats.chunked(MAX_FIELDS).forEach { page ->
			eb.clearFields()
			addStatsPerPlayerSizeField(eb, page)
			resultEmbeds.add(eb.build())
		}

		return resultEmbeds
	}

	fun renderGuildStats(context: Context, stats: GuildStats): List<MessageEmbed> {
		val resultEmbeds = mutableListOf<MessageEmbed>()

		val eb = MessageContext.getDefaultEmbedBuilder()
		val shardManager = context.jda.shardManager
		val guild = shardManager!!.getGuildById(stats.guildId)
		val guildSettings =
			if (guild != null) guildSettingsService.set(guild) else guildSettingsService.guild(stats.guildId)
				.getOrDefault()
		eb.setTitle("${guildSettings.name}'s Wolfia stats")
		eb.setThumbnail(guildSettings.avatarUrl.orElse(null))
		val totalGames = stats.totalWinStats.totalGames
		if (totalGames <= 0) {
			eb.setTitle("There have no games been played in the guild (id `${stats.guildId}`).")
			return listOf(eb.build())
		}

		// stats for all games in this guild
		eb.addBlankField(false)
		eb.addField("Total games played", "$totalGames", true)
		eb.addField("∅ player size", String.format("%.2f", stats.averagePlayerSize.toDouble()), true)
		val baddieWinPercentage = TextchatUtils.divide(stats.totalWinStats.baddieWins, totalGames)
		val goodieWinPercentage = TextchatUtils.divide(stats.totalWinStats.goodieWins, totalGames)
		eb.addField("Win % for ${Emojis.WOLF}", TextchatUtils.percentFormat(baddieWinPercentage), true)
		eb.addField("Win % for ${Emojis.COWBOY}", TextchatUtils.percentFormat(goodieWinPercentage), true)

		// stats by playersize in this guild
		eb.addBlankField(false)
		eb.addField("Stats by player size:", "", false)

		var sortedWinStats: List<WinStats> = ArrayList(stats.winStatsByPlayerSize)
			.sortedWith(Comparator.comparingInt(WinStats::playerSize))

		val takeFields = MAX_FIELDS - eb.fields.count()

		addStatsPerPlayerSizeField(eb, sortedWinStats.take(takeFields))
		resultEmbeds.add(eb.build())

		sortedWinStats = sortedWinStats.drop(takeFields)
		sortedWinStats.chunked(MAX_FIELDS).forEach { page ->
			eb.clearFields()
			addStatsPerPlayerSizeField(eb, page)
			resultEmbeds.add(eb.build())
		}

		return resultEmbeds
	}

	private fun addStatsPerPlayerSizeField(eb: EmbedBuilder, sortedWinStats: List<WinStats>): EmbedBuilder {
		for ((playerSize, totalGames, goodieWins, baddieWins) in sortedWinStats) {
			val baddieWinPercentage = TextchatUtils.percentFormat(TextchatUtils.divide(baddieWins, totalGames))
			val goodieWinPercentage = TextchatUtils.percentFormat(TextchatUtils.divide(goodieWins, totalGames))
			val content = "${Emojis.WOLF} win $baddieWinPercentage\n${Emojis.COWBOY} win $goodieWinPercentage"
			eb.addField(
				"$totalGames games with $playerSize players",
				content, true,
			)
		}
		return eb
	}

	fun renderUserStats(stats: UserStats): EmbedBuilder {
		val eb = MessageContext.getDefaultEmbedBuilder()
		val userAction = userCache.user(stats.userId)
		eb.setTitle("${userAction.name}'s Wolfia stats")
		userAction.fetch().getOrNull()?.avatarUrl?.let { eb.setThumbnail(it) }
		if (stats.totalGames <= 0) {
			eb.setTitle("User (id `${stats.userId}`) hasn't played any games.")
			return eb
		}
		eb.addField("Total games played", "${stats.totalGames}", true)
		eb.addField(
			"Total win %",
			TextchatUtils.percentFormat(TextchatUtils.divide(stats.gamesWon, stats.totalGames)),
			true,
		)
		eb.addField("Games as ${Emojis.WOLF}", "${stats.gamesAsBaddie}", true)
		eb.addField(
			"Win % as ${Emojis.WOLF}",
			TextchatUtils.percentFormat(
				TextchatUtils.divide(
					stats.gamesWonAsBaddie,
					stats.gamesAsBaddie,
				),
			),
			true,
		)
		eb.addField("Games as ${Emojis.COWBOY}", "${stats.gamesAsGoodie}", true)
		eb.addField(
			"Win % as ${Emojis.COWBOY}",
			TextchatUtils.percentFormat(
				TextchatUtils.divide(
					stats.gamesWonAsGoodie,
					stats.gamesAsGoodie,
				),
			),
			true,
		)
		eb.addField("${Emojis.GUN} fired", "${stats.totalShots}", true)
		eb.addField(
			"${Emojis.GUN} accuracy",
			TextchatUtils.percentFormat(TextchatUtils.divide(stats.wolvesShot, stats.totalShots)),
			true,
		)
		eb.addField("Total posts written", "${stats.totalPosts}", true)
		eb.addField("Total post length", "${stats.totalPostLength}", true)
		eb.addField(
			"∅ posts per game",
			"${TextchatUtils.divide(stats.totalPosts, stats.totalGames).toInt()}",
			true,
		)
		eb.addField(
			"∅ post length",
			"${TextchatUtils.divide(stats.totalPostLength, stats.totalPosts).toInt()}",
			true,
		)
		return eb
	}

	fun renderGameStats(stats: GameStats): EmbedBuilder {
		val eb = NiceEmbedBuilder.defaultBuilder()
		val gameId = stats.gameId

		//1. post summary like game, mode, players, roles
		eb.setTitle("**Game #$gameId**")
		eb.setDescription("${stats.gameType.textRep} ${stats.gameMode.textRep}")
		eb.addField("Game started", TextchatUtils.toUtcTime(stats.startTime), true)
		stats.startingTeams.forEach { team ->
			eb.addField(
				if (stats.gameType == Games.POPCORN) team.alignment.textRepWW else team.alignment.textRepMaf,
				team.players.joinToString(", ") { player -> "`${determineNickname(player, stats)}`" },
				true,
			)
		}

		//2. post the actions
		val sortedActions: List<ActionStats> = ArrayList(stats.actions)
			.sortedWith(Comparator.comparingLong(ActionStats::timeStampSubmitted))
		val fieldTitle = "Actions"
		val actionsField = ChunkingField(fieldTitle, false)
		for (action in sortedActions) {
			val actionStr = renderActionStats(action, stats)
			actionsField.add(actionStr, true)
		}
		eb.addField(actionsField)

		//3. post the winners
		eb.addField("Game ended", TextchatUtils.toUtcTime(stats.endTime), true)
		eb.addField("Game length", TextchatUtils.formatMillis(stats.endTime - stats.startTime), true)
		val winText: String
		val winners = stats.startingTeams.firstOrNull(TeamStats::isWinner)
		if (winners == null) {
			//shouldn't happen lol
			logger().error("Game #{} has no winning team in the data", gameId)
			winText = """
				Game has no winning team ${Emojis.WOLFTHINK}
				Replay must be borked. Error has been reported.
				""".trimIndent()
		} else {
			var flavouredTeamName = winners.alignment.textRepMaf
			if (stats.gameType == Games.POPCORN) flavouredTeamName = winners.alignment.textRepWW
			winText = "**Team $flavouredTeamName wins the game!**"
		}
		eb.addField("Winners", winText, true)
		return eb
	}

	fun renderActionStats(actionStats: ActionStats, game: GameStats): String {
		val gameId = game.gameId
		val timeSinceGameStarted = TextchatUtils.formatMillis(actionStats.timeStampHappened - game.startTime)
		val targetNick = { getFormattedNickFromStats(game, actionStats.target) }
		val actorNick = { getFormattedNickFromStats(game, actionStats.actor) }

		val result = "`$timeSinceGameStarted` " + when (actionStats.actionType) {
			Actions.GAMESTART -> "${Emojis.VIDEO_GAME}: Game **#$gameId** starts."
			Actions.GAMEEND -> "${Emojis.END}: Game **#$gameId** ends."
			Actions.DAYSTART -> "${Emojis.SUNNY}: Day **${actionStats.cycle}** starts."
			Actions.DAYEND -> "${Emojis.CITY_SUNSET_SUNRISE}: Day **${actionStats.cycle}** ends."
			Actions.NIGHTSTART -> "${Emojis.FULL_MOON}: Night **${actionStats.cycle}** starts."
			Actions.NIGHTEND -> "${Emojis.CITY_SUNSET_SUNRISE}: Night **${actionStats.cycle}** ends."
			Actions.BOTKILL -> "${Emojis.SKULL}: ${targetNick.invoke()} botkilled."
			Actions.MODKILL -> "${Emojis.COFFIN}: ${targetNick.invoke()} modkilled."
			Actions.DEATH -> "${Emojis.RIP}: ${targetNick.invoke()} dies."
			Actions.VOTELYNCH -> "${Emojis.BALLOT_BOX}: ${actorNick.invoke()} votes to lynch ${targetNick.invoke()}."
			Actions.LYNCH -> "${Emojis.FIRE}: ${targetNick.invoke()} is lynched"
			Actions.VOTENIGHTKILL -> "${Emojis.BALLOT_BOX}: ${actorNick.invoke()} votes to night kill ${targetNick.invoke()}."
			Actions.CHECK -> "${Emojis.MAGNIFIER}: ${actorNick.invoke()} checks alignment of ${targetNick.invoke()}."
			Actions.SHOOT -> "${Emojis.GUN}: ${actorNick.invoke()} shoots ${targetNick.invoke()}."
			Actions.VOTEGUN -> "${Emojis.BALLOT_BOX}: ${actorNick.invoke()} votes to give ${targetNick.invoke()} the ${Emojis.GUN}."
			Actions.GIVEGUN -> "${Emojis.GUN}: ${targetNick.invoke()} receives the gun"
			Actions.GIVE_PRESENT -> "${Item.ItemType.PRESENT}: ${actorNick.invoke()} gives ${targetNick.invoke()} a present"
			Actions.OPEN_PRESENT -> "${Item.ItemType.PRESENT}: ${targetNick.invoke()} opens a present and receives a ${Item.ItemType.valueOf(actionStats.additionalInfo!!)}"
		}
		return result
	}

	private fun getFormattedNickFromStats(gameStats: GameStats, userId: Long): String {
		val baddieEmoji = if (gameStats.gameType == Games.POPCORN) Emojis.WOLF else Emojis.SPY
		for (team in gameStats.startingTeams) {
			for (player in team.players) {
				if (player.userId == userId) {
					val nickname = determineNickname(player, gameStats)
					return "`$nickname` ${if (player.alignment == Alignments.VILLAGE) Emojis.COWBOY else baddieEmoji}"
				}
			}
		}
		val message = "No such player $userId in this game ${gameStats.gameId}"
		logger().error(message, IllegalArgumentException(message))
		return Player.UNKNOWN_NAME
	}

	private fun determineNickname(player: PlayerStats, gameStats: GameStats): String {
		val nickname = player.nickname
		if (nickname != null) {
			return nickname
		}
		val allPlayers = gameStats.startingTeams.flatMap { it.players }
		require(allPlayers.isNotEmpty()) { "Empty list of players for game ${gameStats.gameId}" }
		require(allPlayers.contains(player)) { "Player list of game #${gameStats.gameId} does not contain user ${player.userId}" }

		val left = allPlayers
			.sortedWith(Comparator.comparingLong(PlayerStats::playerId))
			.dropWhile { it.playerId != player.playerId }
			.count()
		val playerNumber = allPlayers.size - left + 1
		return "Player#$playerNumber"
	}
}
