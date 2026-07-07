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
package space.npstr.wolfia.ecs

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile
import space.npstr.wolfia.system.logger

/**
 * A per-game-instance container that holds entities, systems, and serializes event dispatch.
 * All mutation happens on a single dispatch thread - systems need no synchronization.
 *
 * External threads (e.g. JDA event listeners) call [.submit] which is non-blocking
 * and returns a [CompletableFuture] for optional awaiting (useful in tests).
 */
class GameWorld(
	val channelId: Long,
) {

	val entities = mutableListOf<GameEntity>()
	private val systems = mutableListOf<GameSystem>()
	private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
		Thread(r, "game-world-$channelId")
			.also { it.isDaemon = true }
	}

	@Volatile
	var isActive: Boolean = true
		private set

	// --- Event dispatch ---
	/**
	 * Submit an event for processing. Called from external threads (e.g. JDA).
	 * Non-blocking. Returns a future that completes when all systems have processed the event.
	 */
	fun submit(gameEvent: GameEvent): CompletableFuture<Unit> {
		if (!this.isActive) {
			return CompletableFuture.failedFuture<Unit>(
				IllegalStateException("GameWorld for channel $channelId is no longer active")
			)
		}
		val future = CompletableFuture<Unit>()
		executor.execute {
			try {
				dispatch(gameEvent)
				future.complete(Unit)
			} catch (e: Exception) {
				logger().error(
					"Error dispatching event {} in game world {}",
					gameEvent.javaClass.getSimpleName(), channelId, e
				)
				future.completeExceptionally(e)
			}
		}
		return future
	}

	private fun dispatch(gameEvent: GameEvent) {
		for (system in systems.toList()) {
			if (!this.isActive) break
			system.process(gameEvent, this)
		}
	}

	// --- Entity management ---
	fun addEntity(entity: GameEntity) {
		entities.add(entity)
	}

	fun removeEntity(entity: GameEntity): Boolean {
		return entities.remove(entity)
	}

	inline fun <reified T : GameComponent> findEntitiesWith(): List<GameEntity> {
		return entities.filter { it.hasComponent<T>() }
	}

	fun entities(): List<GameEntity> {
		return entities.toList()
	}

	// --- System management ---
	fun addSystem(system: GameSystem) {
		systems.add(system)
	}

	// --- Lifecycle ---
	fun close() {
		this.isActive = false
		// Run close on the dispatch thread to avoid races with in-flight events
		try {
			executor.submit {
				for (system in systems) {
					try {
						system.close(this)
					} catch (e: Exception) {
						logger().error(
							"Error closing system {} in game world {}",
							system.javaClass.getSimpleName(), channelId, e
						)
					}
				}
			}.get(10, TimeUnit.SECONDS)
		} catch (e: Exception) {
			logger().error("Error during game world {} shutdown", channelId, e)
		}
		executor.shutdown()
	}
}
