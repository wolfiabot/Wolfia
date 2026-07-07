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

import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class GameWorldTest {

	internal class PingEvent : GameEvent

	internal class PongEvent : GameEvent

	internal data class NameComponent(val name: String) : GameComponent

	internal data class CounterComponent(val count: AtomicInteger) : GameComponent

	private lateinit var world: GameWorld

	@AfterEach
	fun tearDown() {
		world.close()
	}

	@Test
	fun systemsProcessedInRegistrationOrder() {
		world = GameWorld(1L)
		val order = mutableListOf<String>()

		world.addSystem { _, _ -> order.add("first") }
		world.addSystem { _, _ -> order.add("second") }
		world.addSystem { _, _ -> order.add("third") }

		world.submit(PingEvent()).get(5, TimeUnit.SECONDS)

		assertThat(order).containsExactly("first", "second", "third")
	}

	@Test
	fun systemsReceiveCorrectEventType() {
		world = GameWorld(1L)
		val events = mutableListOf<String>()

		world.addSystem { gameEvent: GameEvent, _ -> events.add(gameEvent.javaClass.getSimpleName()) }

		world.submit(PingEvent()).get(5, TimeUnit.SECONDS)
		world.submit(PongEvent()).get(5, TimeUnit.SECONDS)

		assertThat(events).containsExactly("PingEvent", "PongEvent")
	}

	@Test
	fun systemsCanReadAndMutateEntities() {
		world = GameWorld(1L)
		val entity = GameEntity()
		val counter = CounterComponent(AtomicInteger(0))
		entity.addComponent(counter)
		world.addEntity(entity)

		world.addSystem { _, world: GameWorld ->
			for (e in world.findEntitiesWith<CounterComponent>()) {
				requireNotNull(e.findComponent<CounterComponent>()).count.incrementAndGet()
			}
		}

		world.submit(PingEvent()).get(5, TimeUnit.SECONDS)
		world.submit(PingEvent()).get(5, TimeUnit.SECONDS)

		assertThat(counter.count.get()).isEqualTo(2)
	}

	@Test
	fun eventsAreSerializedPerWorld() {
		world = GameWorld(1L)
		val maxConcurrent = AtomicInteger(0)
		val current = AtomicInteger(0)

		world.addSystem { _, _ ->
			val c = current.incrementAndGet()
			maxConcurrent.updateAndGet { max: Int -> max(max, c) }
			try {
				Thread.sleep(10)
			} catch (_: InterruptedException) {
				Thread.currentThread().interrupt()
			}
			current.decrementAndGet()
		}

		// Submit many events concurrently from different threads
		val eventCount = 20
		val latch = CountDownLatch(1)
		val futures = mutableListOf<CompletableFuture<Unit>>()
		for (i in 0..<eventCount) {
			CompletableFuture.runAsync {
				try {
					latch.await()
				} catch (_: InterruptedException) {
					Thread.currentThread().interrupt()
				}
				futures.add(world.submit(PingEvent()))
			}
		}
		latch.countDown()
		Thread.sleep(100) // let all submits happen
		CompletableFuture.allOf(*futures.toTypedArray()).get(10, TimeUnit.SECONDS)

		assertThat(maxConcurrent.get()).isEqualTo(1)
	}

	@Test
	fun findEntitiesWithComponent() {
		world = GameWorld(1L)
		val withName = GameEntity()
		withName.addComponent(NameComponent("Alice"))
		val withoutName = GameEntity()
		withoutName.addComponent(CounterComponent(AtomicInteger(0)))
		val alsoWithName = GameEntity()
		alsoWithName.addComponent(NameComponent("Bob"))

		world.addEntity(withName)
		world.addEntity(withoutName)
		world.addEntity(alsoWithName)

		val result = world.findEntitiesWith<NameComponent>()
		assertThat(result).containsExactly(withName, alsoWithName)
	}

	@Test
	fun removeEntity() {
		world = GameWorld(1L)
		val entity = GameEntity()
		world.addEntity(entity)

		assertThat(world.removeEntity(entity)).isTrue()
		assertThat(world.removeEntity(entity)).isFalse()
	}

	@Test
	fun entitiesReturnsUnmodifiableList() {
		world = GameWorld(1L)
		world.addEntity(GameEntity())

		val entities = world.entities()
		assertThat(entities).hasSize(1)

		assertThrows(
			UnsupportedOperationException::class.java
		) { (entities as MutableList).add(GameEntity()) }
	}

	@Test
	fun submitAfterCloseReturnsFailedFuture() {
		world = GameWorld(1L)
		world.close()

		val future = world.submit(PingEvent())
		assertThat(future.isCompletedExceptionally()).isTrue()
	}

	@Test
	fun closeCallsSystemClose() {
		world = GameWorld(1L)
		val closed = Collections.synchronizedList(ArrayList<String>())

		world.addSystem(object : GameSystem {
			override fun process(gameEvent: GameEvent, world: GameWorld) {}

			override fun close(world: GameWorld) {
				closed.add("system-closed")
			}
		})

		world.close()
		assertThat(closed).containsExactly("system-closed")
	}

	@Test
	fun channelIdIsAccessible() {
		world = GameWorld(42L)
		assertThat(world.channelId).isEqualTo(42L)
	}

	@Test
	fun systemExceptionDoesNotBreakOtherSystems() {
		world = GameWorld(1L)
		val order = Collections.synchronizedList(ArrayList<String>())

		world.addSystem { _, _ -> order.add("before") }
		world.addSystem { _, _ -> throw RuntimeException("boom") }
		world.addSystem { _, _ -> order.add("after") }

		// The future should complete exceptionally but systems before the failure should have run
		val future = world.submit(PingEvent())
		assertThatThrownBy { future.get(5, TimeUnit.SECONDS) }
			.isInstanceOf(ExecutionException::class.java)
			.hasCauseInstanceOf(RuntimeException::class.java)

		assertThat(order).contains("before")
	}
}
