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

/**
 * Processes [GameEvent]s against the entities in a [GameWorld].
 * Systems are called in registration order for each event dispatch.
 */
fun interface GameSystem {

	/**
	 * Called for each event dispatched to the game world.
	 * Systems run on the GameWorld's single dispatch thread - no synchronization needed.
	 */
	fun process(gameEvent: GameEvent, world: GameWorld)

	/**
	 * Called when the game world is shutting down. Release any resources.
	 */
	fun close(world: GameWorld) {}
}
