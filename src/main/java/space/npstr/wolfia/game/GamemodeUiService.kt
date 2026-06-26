/*
 * Copyright (C) 2016-2026 the original author or authors
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
package space.npstr.wolfia.game

import org.springframework.stereotype.Service
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Games

/**
 * Exposes the static information about the supported games and their modes for the public web API.
 *
 * The structural facts (supported modes, default mode, acceptable player counts, required permissions) are read
 * straight from the [GameInfo] implementations, the source of truth used by the running games. The narrative rules
 * are authored here, since they do not otherwise exist in code (they used to live on the old static website).
 *
 * Role tokens like `:wolf:` in the descriptions are replaced with role icons by the frontend.
 */
@Service
class GamemodeUiService {

	// presentation order, matching how players think about the games
	private val gameOrder = listOf(Games.MAFIA, Games.POPCORN)

	data class GamemodeUiInfo(
		val name: String,
		val description: String,
		val modes: List<ModeUiInfo>,
	)

	data class ModeUiInfo(
		val name: String,
		val isDefault: Boolean,
		val playerCount: String,
		val description: String,
	)

	fun getGamemodes(): List<GamemodeUiInfo> {
		return gameOrder.map { game ->
			val info = Games.getInfo(game)
			GamemodeUiInfo(
				name = game.textRep,
				description = gameDescription(game),
				modes = info.supportedModes.map { mode -> toModeInfo(info, mode) },
			)
		}
	}

	private fun toModeInfo(info: GameInfo, mode: GameMode): ModeUiInfo {
		return ModeUiInfo(
			name = mode.textRep,
			isDefault = mode == info.defaultMode,
			playerCount = info.getAcceptablePlayerNumbers(mode),
			description = modeDescription(mode),
		)
	}

	private fun gameDescription(game: Games): String = when (game) {
		Games.MAFIA -> """
			Town :townie: against Mafia :mafia:

			- The Mafia knows their team, they receive an invite to mafia chat with their role pms
			- During the day everyone votes to lynch one of the players
			- During the night the Mafia kills players
			- Town wins when all Mafia are dead, Mafia wins when they reach parity.
		""".trimIndent()

		Games.POPCORN -> """
			Village :villager: against Wolves :wolf:

			- The :wolf:s know their team.
			- A :villager: holds the :gun:.
			- If the :villager: shoots a :wolf:, the :wolf: dies and the :villager: can shoot again.
			- If the :villager: shoots another :villager:, the shooter dies, and the :gun: goes to the :villager: that was shot at.
			- :villager:s win when all :wolf:s are dead, :wolf:s win when they reach parity.
		""".trimIndent()
	}

	private fun modeDescription(mode: GameMode): String = when (mode) {
		GameMode.LITE ->
			"Power roles: The Cop :cop: investigates the alignment of a player at night."

		GameMode.XMAS -> """
			Power roles: The Cop :cop: investigates the alignment of a player at night.
			(Many) Santas :santa: who give out presents to other players, which contain useful, but also dangerous items.
		""".trimIndent()

		GameMode.PURE ->
			"No power roles, just a plain game of villagers against wolves."

		GameMode.WILD -> """
			The Wild mode randomizes who gets the :gun:.
			The channel is never be closed, non-players and dead players can post all the time.
		""".trimIndent()

		GameMode.CLASSIC -> """
			The Classic mode allows the :wolf:s to have a separate hidden chat, where they may decide which :villager: gets the :gun:.
			The game channel is moderated, which means during a game only the living players are allowed to talk in the channel.
		""".trimIndent()
	}
}
