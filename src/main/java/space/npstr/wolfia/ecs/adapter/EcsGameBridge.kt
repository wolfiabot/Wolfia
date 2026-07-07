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
package space.npstr.wolfia.ecs.adapter

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component
import space.npstr.wolfia.ecs.GameWorld

/**
 * Tracks active ECS game worlds by channel ID. Parallel to [space.npstr.wolfia.domain.game.GameRegistry]
 * for the legacy path.
 */
@Component
class EcsGameBridge {

	private val worlds = ConcurrentHashMap<Long, GameWorld>()

	fun get(channelId: Long): GameWorld? {
		return worlds.get(channelId)
	}

	fun register(world: GameWorld) {
		worlds[world.channelId] = world
	}

	/**
	 * Unregisters a game world from the bridge without closing it.
	 * Used by GameOverCleanupSystem when a game ends naturally
	 * (the world is still processing the current event dispatch).
	 */
	fun unregister(channelId: Long) {
		worlds.remove(channelId)
	}

	/**
	 * Removes and closes a game world. Used for forced cleanup (e.g. shutdown).
	 */
	fun remove(channelId: Long) {
		val world = worlds.remove(channelId)
		world?.close()
	}

	fun hasGame(channelId: Long): Boolean {
		val world = worlds.get(channelId)
		return world != null && world.isActive
	}

	val runningGamesCount: Int
		get() = worlds.size
}
