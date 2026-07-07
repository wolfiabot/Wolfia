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
package space.npstr.wolfia.ecs.popcorn

import java.time.Duration
import space.npstr.wolfia.ecs.GameEvent
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Alignments

/**
 * All events for the Popcorn game mode.
 */
object PopcornEvents {

	/**
	 * Starts a new Popcorn game.
	 *
	 * @param channelId the Discord channel hosting the game
	 * @param guildId   the Discord guild
	 * @param players   player user IDs paired with their alignment (pre-assigned for testability)
	 * @param mode      WILD or CLASSIC
	 * @param dayLength how long the gun bearer has to shoot
	 */
	data class GameStartEvent(
		val channelId: Long,
		val guildId: Long,
		val players: MutableList<PlayerSetup>,
		val mode: GameMode,
		val dayLength: Duration,
	) : GameEvent


	data class PlayerSetup(val userId: Long, val alignment: Alignments)


	data class ShootEvent(val shooterId: Long, val targetId: Long) : GameEvent


	data class TimerExpiredEvent(val day: Int) : GameEvent


	data class GunDistVoteEvent(val voterId: Long, val candidateId: Long) : GameEvent

	class GunDistTimerExpiredEvent : GameEvent

	/**
	 * Internal event: gun distribution resolved, gun given to a player, start the day.
	 */
	data class GunGivenEvent(val recipientId: Long) : GameEvent
}
