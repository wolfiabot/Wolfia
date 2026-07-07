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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class GameEntityTest {

	internal data class HealthComponent(val hp: Int) : GameComponent

	internal data class NameComponent(val name: String?) : GameComponent

	internal class TagComponent : GameComponent

	@Test
	fun addAndFindComponent() {
		val entity = GameEntity()
		val health = HealthComponent(100)
		entity.addComponent(health)

		assertThat(entity.findComponent<HealthComponent>()).isSameAs(health)
	}

	@Test
	fun findComponentReturnsNullWhenAbsent() {
		val entity = GameEntity()

		assertThat(entity.findComponent<HealthComponent>()).isNull()
	}

	@Test
	fun findComponentsReturnsAll() {
		val entity = GameEntity()
		val tag1 = TagComponent()
		val tag2 = TagComponent()
		entity.addComponent(tag1)
		entity.addComponent(tag2)

		val tags = entity.findComponents<TagComponent>()
		assertThat(tags).containsExactly(tag1, tag2)
	}

	@Test
	fun hasComponent() {
		val entity = GameEntity()
		assertThat(entity.hasComponent<HealthComponent>()).isFalse()

		entity.addComponent(HealthComponent(50))
		assertThat(entity.hasComponent<HealthComponent>()).isTrue()
	}

	@Test
	fun removeComponentByReference() {
		val entity = GameEntity()
		val health = HealthComponent(100)
		entity.addComponent(health)

		val removed = entity.removeComponent(health)
		assertThat(removed).isTrue()
		assertThat(entity.findComponent<HealthComponent>()).isNull()
	}

	@Test
	fun removeComponentByReferenceReturnsFalseWhenAbsent() {
		val entity = GameEntity()
		val health = HealthComponent(100)

		assertThat(entity.removeComponent(health)).isFalse()
	}

	@Test
	fun removeComponentsByType() {
		val entity = GameEntity()
		val tag1 = TagComponent()
		val tag2 = TagComponent()
		val health = HealthComponent(42)
		entity.addComponent(tag1)
		entity.addComponent(health)
		entity.addComponent(tag2)

		entity.removeComponents<TagComponent>()
		assertThat(entity.hasComponent<TagComponent>()).isFalse()
		assertThat(entity.findComponent<HealthComponent>()).isSameAs(health)
	}
}
