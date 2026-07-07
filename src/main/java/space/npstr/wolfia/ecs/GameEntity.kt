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
 * An identity with an associated bag of [GameComponent]s.
 * The unique composition of components on an entity defines its behaviour.
 */
class GameEntity {

	val components = mutableListOf<GameComponent>()

	fun addComponent(component: GameComponent) {
		components.add(component)
	}


	fun removeComponent(component: GameComponent): Boolean {
		return components.remove(component)
	}

	inline fun <reified T : GameComponent> removeComponents() {
		components.removeAll { it::class == T::class }
	}

	inline fun <reified T : GameComponent> findComponent(): T? {
		return components.filterIsInstance<T>().firstOrNull()
	}

	inline fun <reified T : GameComponent> findComponents(): List<T> {
		return components.filterIsInstance<T>()
	}

	inline fun <reified T : GameComponent> hasComponent(): Boolean {
		return findComponent<T>() != null
	}
}
