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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import space.npstr.wolfia.ecs.GameEntity
import space.npstr.wolfia.ecs.GameEvent
import space.npstr.wolfia.ecs.GameSystem
import space.npstr.wolfia.ecs.GameWorld
import space.npstr.wolfia.ecs.OutputMessage.Companion.toGameChannel
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameMetaComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunDistributionComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunHolderComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PhaseComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PlayerComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.TimerRequestComponent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GunDistTimerExpiredEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.TimerExpiredEvent
import space.npstr.wolfia.game.GameInfo.GameMode

/**
 * Manage timers for Popcorn games.
 *
 * On each event dispatch:
 * 1. Check for TimerRequestComponents and schedule them on the executor
 * 2. Handle TimerExpiredEvent: modkill the gun bearer if the day matches
 */
class TimerSystem(
	/**
	 * @param scheduler the executor for scheduling timer callbacks. Null in tests
	 * (tests submit TimerExpiredEvent directly).
	 */
	private val scheduler: ScheduledExecutorService?,
) : GameSystem {
	private val activeFutures = mutableMapOf<String, ScheduledFuture<*>>()

	override fun process(gameEvent: GameEvent, world: GameWorld) {
		// 1. Process any pending timer requests
		consumeTimerRequests(world)

		// 2. Handle timer expiry
		if (gameEvent is TimerExpiredEvent) {
			handleDayTimerExpired(gameEvent, world)
		}
	}

	private fun consumeTimerRequests(world: GameWorld) {
		for (entity in world.findEntitiesWith<TimerRequestComponent>()) {
			for (request in entity.findComponents<TimerRequestComponent>()) {
				scheduleTimer(world, request.delay, request.gameEventToFire)
			}
			entity.removeComponents<TimerRequestComponent>()
		}
	}

	private fun scheduleTimer(world: GameWorld, delay: Duration, gameEventToFire: GameEvent) {
		if (scheduler == null) return  // in tests, events are submitted directly

		val key = gameEventToFire.javaClass.getSimpleName() + "-" + System.nanoTime()
		val future: ScheduledFuture<*> = scheduler.schedule<CompletableFuture<Unit>>(
			{ world.submit(gameEventToFire) },
			delay.toMillis(),
			TimeUnit.MILLISECONDS
		)
		activeFutures[key] = future
	}

	private fun handleDayTimerExpired(timerExpired: TimerExpiredEvent, world: GameWorld) {
		if (!PopcornHelper.isGameRunning(world)) return

		val gameState = PopcornHelper.findGameStateEntity(world)
			?: return
		val phase = gameState.findComponent<PhaseComponent>()
			?: return

		// Stale timer: day has already moved on
		if (timerExpired.day != phase.day()) return

		val queue = PopcornHelper.messageQueue(world)
			?: return

		// Modkill the gun bearer
		val holderEntity = PopcornHelper.gunHolder(world)
			?: return
		val holderPc = holderEntity.findComponent<PlayerComponent>()
			?: return

		holderPc.kill()
		holderEntity.removeComponents<GunHolderComponent>()

		queue.add(toGameChannel(String.format("Day %d has ended!", phase.day())))
		queue.add(
			toGameChannel(
				String.format(
					"%s took too long to decide who to shoot! They died and the gun will be redistributed.",
					PopcornHelper.mention(holderPc.userId())
				)
			)
		)

		// Redistribute gun (checked by WinConditionSystem first in next dispatch,
		// but we set up redistribution here since systems process in order within this dispatch)
		val meta = gameState.findComponent<GameMetaComponent>()
		if (meta != null && meta.mode() == GameMode.WILD) {
			// WILD: give to first living villager
			val villagers = PopcornHelper.livingVillagers(world)
			if (!villagers.isEmpty()) {
				val newHolder: GameEntity = villagers.first()
				newHolder.addComponent(GunHolderComponent())
				val newHolderPc = newHolder.findComponent<PlayerComponent>()
				val newHolderId = newHolderPc?.userId() ?: 0
				queue.add(
					toGameChannel(
						String.format("%s has received the gun!", PopcornHelper.mention(newHolderId))
					)
				)
				phase.startNewDay()
				queue.add(
					toGameChannel(
						String.format(
							"Day %d started! %s, you have %d minutes to shoot someone.",
							phase.day(), PopcornHelper.mention(newHolderId), phase.dayLengthMillis() / 60000
						)
					)
				)
				gameState.addComponent(
					TimerRequestComponent(
						Duration.ofMillis(phase.dayLengthMillis()),
						TimerExpiredEvent(phase.day())
					)
				)
			}
		} else {
			// CLASSIC: wolves redistribute
			gameState.addComponent(GunDistributionComponent())
			queue.add(
				toGameChannel(
					"Wolves are distributing the gun! Please stand by."
				)
			)
			gameState.addComponent(
				TimerRequestComponent(
					Duration.ofSeconds(60),
					GunDistTimerExpiredEvent()
				)
			)
		}
	}

	override fun close(world: GameWorld) {
		for (future in activeFutures.values) {
			future.cancel(false)
		}
		activeFutures.clear()
	}
}
