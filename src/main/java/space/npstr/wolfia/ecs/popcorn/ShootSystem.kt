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
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.ShootEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.TimerExpiredEvent
import space.npstr.wolfia.game.GameInfo.GameMode

/**
 * Handle ShootEvent: validate the shot, resolve the kill, and set up the next phase.
 *
 *
 * Rules:
 * - Shooter must hold the gun, be alive, and be in the game
 * - Target must be alive and in the game, and not the shooter
 * - If target is a wolf: target dies, shooter keeps gun, new day starts
 * - If target is a villager: SHOOTER dies (not the target), gun is redistributed
 */
class ShootSystem : GameSystem {
	override fun process(gameEvent: GameEvent, world: GameWorld) {
		if (gameEvent !is ShootEvent) return
		if (!PopcornHelper.isGameRunning(world)) return

		val queue = PopcornHelper.messageQueue(world)
			?: return

		val shooterId = gameEvent.shooterId
		val targetId = gameEvent.targetId

		// --- Validation ---
		val shooterEntity = PopcornHelper.findPlayerEntity(world, shooterId)
		if (shooterEntity == null) {
			queue.add(
				toGameChannel(
					String.format("%s shush, you're not playing in this game!", PopcornHelper.mention(shooterId))
				)
			)
			return
		}
		val shooterPc = shooterEntity.findComponent<PlayerComponent>()
		if (shooterPc == null || !shooterPc.isAlive) {
			queue.add(
				toGameChannel(
					String.format("%s shush, you're dead!", PopcornHelper.mention(shooterId))
				)
			)
			return
		}
		if (shooterId == targetId) {
			queue.add(
				toGameChannel(
					String.format(
						"%s please don't shoot yourself, that would make a big mess.",
						PopcornHelper.mention(shooterId)
					)
				)
			)
			return
		}
		if (!shooterEntity.hasComponent<GunHolderComponent>()) {
			queue.add(
				toGameChannel(
					String.format("%s you do not have the gun!", PopcornHelper.mention(shooterId))
				)
			)
			return
		}

		val targetEntity = PopcornHelper.findPlayerEntity(world, targetId)
		if (targetEntity == null) {
			queue.add(
				toGameChannel(
					String.format(
						"%s you have to shoot a living player of this game!",
						PopcornHelper.mention(shooterId)
					)
				)
			)
			return
		}
		val targetPc = targetEntity.findComponent<PlayerComponent>()
		if (targetPc == null || !targetPc.isAlive) {
			queue.add(
				toGameChannel(
					String.format(
						"%s you have to shoot a living player of this game!",
						PopcornHelper.mention(shooterId)
					)
				)
			)
			return
		}

		// --- Resolution ---
		val gameState = PopcornHelper.findGameStateEntity(world)
		val phase = gameState!!.findComponent<PhaseComponent>()
		val meta = gameState.findComponent<GameMetaComponent>()

		queue.add(toGameChannel(String.format("Day %d has ended!", phase!!.day())))

		// Remove gun from shooter
		shooterEntity.removeComponents<GunHolderComponent>()

		if (targetPc.isWolf) {
			// Shot a wolf: target dies
			targetPc.kill()
			queue.add(
				toGameChannel(
					String.format("%s was a dirty wolf!", PopcornHelper.mention(targetId))
				)
			)

			// Shooter keeps gun, new day starts (unless game over — checked by WinConditionSystem)
			shooterEntity.addComponent(GunHolderComponent())
			phase.startNewDay()
			queue.add(
				toGameChannel(
					String.format(
						"Day %d started! %s, you have %d minutes to shoot someone.",
						phase.day(), PopcornHelper.mention(shooterId), phase.dayLengthMillis() / 60000
					)
				)
			)
			gameState.addComponent(
				TimerRequestComponent(
					Duration.ofMillis(phase.dayLengthMillis()),
					TimerExpiredEvent(phase.day())
				)
			)
		} else {
			// Shot a villager: SHOOTER dies
			shooterPc.kill()
			queue.add(
				toGameChannel(
					String.format(
						"%s is an innocent villager! %s dies.",
						PopcornHelper.mention(targetId),
						PopcornHelper.mention(shooterId)
					)
				)
			)

			// Gun goes to the survivor (target), needs redistribution
			// In WILD mode: give to a random living villager (the target is alive and a villager)
			// In CLASSIC mode: wolves vote
			if (meta!!.mode() == GameMode.WILD) {
				// Give gun to the target (the surviving villager who was shot at)
				targetEntity.addComponent(GunHolderComponent())
				queue.add(
					toGameChannel(
						String.format("%s has received the gun!", PopcornHelper.mention(targetId))
					)
				)
				phase.startNewDay()
				queue.add(
					toGameChannel(
						String.format(
							"Day %d started! %s, you have %d minutes to shoot someone.",
							phase.day(), PopcornHelper.mention(targetId), phase.dayLengthMillis() / 60000
						)
					)
				)
				gameState.addComponent(
					TimerRequestComponent(
						Duration.ofMillis(phase.dayLengthMillis()),
						TimerExpiredEvent(phase.day())
					)
				)
			} else {
				// CLASSIC mode: wolves redistribute
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
	}
}
