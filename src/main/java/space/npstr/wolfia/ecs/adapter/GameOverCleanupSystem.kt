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

import space.npstr.wolfia.ecs.GameEvent
import space.npstr.wolfia.ecs.GameSystem
import space.npstr.wolfia.ecs.GameWorld
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameOverComponent

/**
 * Detects when a game ends ([GameOverComponent] is present) and schedules
 * removal of the [GameWorld] from the [EcsGameBridge].
 */
class GameOverCleanupSystem(
	private val bridge: EcsGameBridge,
) : GameSystem {

	private var cleaned = false

	override fun process(gameEvent: GameEvent, world: GameWorld) {
		if (cleaned) return

		// TODO wtf!
		for (entity in world.findEntitiesWith<GameOverComponent>()) {
			cleaned = true
			// Remove from bridge after current dispatch completes
			// (the MessageFlushSystem still needs to run after us)
			// We just unregister from the bridge; the world stays active
			// until close() is called explicitly or it's garbage collected.
			bridge.unregister(world.channelId)
			return
		}
	}
}
