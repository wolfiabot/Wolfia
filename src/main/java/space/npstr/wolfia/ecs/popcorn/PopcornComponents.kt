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
import space.npstr.wolfia.ecs.GameComponent
import space.npstr.wolfia.ecs.GameEvent
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Alignments

/**
 * All components for the Popcorn game mode.
 */
object PopcornComponents {
	/**
	 * Attached to each player entity.
	 */
	class PlayerComponent(private val userId: Long, private val number: Int, private val alignment: Alignments) :
		GameComponent {
		var isAlive: Boolean = true
			private set

		fun userId(): Long {
			return userId
		}

		fun number(): Int {
			return number
		}

		fun alignment(): Alignments {
			return alignment
		}

		fun kill() {
			this.isAlive = false
		}

		val isVillage: Boolean
			get() = alignment == Alignments.VILLAGE

		val isWolf: Boolean
			get() = alignment == Alignments.WOLF
	}

	/**
	 * Marker: this player entity currently holds the gun.
	 */
	class GunHolderComponent : GameComponent

	/**
	 * Game phase tracking. Attached to the game-state entity.
	 */
	class PhaseComponent(private val dayLengthMillis: Long) : GameComponent {
		private var day = 0
		private var dayStartedMillis: Long = 0

		fun day(): Int {
			return day
		}

		fun dayStartedMillis(): Long {
			return dayStartedMillis
		}

		fun dayLengthMillis(): Long {
			return dayLengthMillis
		}

		fun startNewDay() {
			day++
			dayStartedMillis = System.currentTimeMillis()
		}
	}

	/**
	 * Game metadata. Attached to the game-state entity.
	 */
	class GameMetaComponent(private val channelId: Long, private val guildId: Long, private val mode: GameMode) :
		GameComponent {
		var isRunning: Boolean = true
			private set

		fun channelId(): Long {
			return channelId
		}

		fun guildId(): Long {
			return guildId
		}

		fun mode(): GameMode {
			return mode
		}

		fun stop() {
			this.isRunning = false
		}
	}

	/**
	 * Gun distribution state. Transient: added when distribution starts, removed when done.
	 */
	class GunDistributionComponent : GameComponent {
		private val votes = mutableMapOf<Long, Long>()
		private val startedMillis: Long = System.currentTimeMillis()

		var isDone: Boolean = false
			private set

		fun votes(): MutableMap<Long, Long> {
			return votes
		}

		fun startedMillis(): Long {
			return startedMillis
		}

		fun markDone() {
			this.isDone = true
		}

		fun vote(voterId: Long, candidateId: Long) {
			votes.remove(voterId)
			votes[voterId] = candidateId
		}
	}

	/**
	 * Requests a timer. Consumed by TimerSystem.
	 */
	data class TimerRequestComponent(val delay: Duration, val gameEventToFire: GameEvent) :
		GameComponent

	/**
	 * Terminal: game is over.
	 */

	data class GameOverComponent(val winner: Alignments) : GameComponent
}
