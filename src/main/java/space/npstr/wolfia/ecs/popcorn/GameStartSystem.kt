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
import space.npstr.wolfia.ecs.GameEntity
import space.npstr.wolfia.ecs.GameEvent
import space.npstr.wolfia.ecs.GameSystem
import space.npstr.wolfia.ecs.GameWorld
import space.npstr.wolfia.ecs.MessageQueueComponent
import space.npstr.wolfia.ecs.OutputMessage.Companion.toDm
import space.npstr.wolfia.ecs.OutputMessage.Companion.toGameChannel
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameMetaComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunDistributionComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunHolderComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PhaseComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PlayerComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.TimerRequestComponent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GameStartEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GunDistTimerExpiredEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.TimerExpiredEvent
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Alignments

/**
 * Handle GameStartEvent: create player entities, game-state entity, send role PMs,
 * and initiate gun distribution.
 */
class GameStartSystem : GameSystem {

	override fun process(gameEvent: GameEvent, world: GameWorld) {
		if (gameEvent !is GameStartEvent) return

		// Create game-state entity
		val gameState = GameEntity()
		gameState.addComponent(GameMetaComponent(gameEvent.channelId, gameEvent.guildId, gameEvent.mode))
		gameState.addComponent(PhaseComponent(gameEvent.dayLength.toMillis()))
		gameState.addComponent(MessageQueueComponent())
		world.addEntity(gameState)

		val queue = gameState.findComponent<MessageQueueComponent>()

		// Create player entities
		var playerNumber = 1
		val wolfTeam = StringBuilder("Your team is:\n")
		val wolves = gameEvent.players.stream()
			.filter { it.alignment == Alignments.WOLF }
			.toList()
		for (wolf in wolves) {
			wolfTeam.append(PopcornHelper.mention(wolf.userId)).append("\n")
		}

		for (setup in gameEvent.players) {
			val playerEntity = GameEntity()
			playerEntity.addComponent(PlayerComponent(setup.userId, playerNumber, setup.alignment))
			world.addEntity(playerEntity)

			// Send role PM
			val rolePm = StringBuilder()
			rolePm.append(setup.alignment.rolePmBlockWW).append("\n")
			if (setup.alignment == Alignments.VILLAGE) {
				rolePm.append("If you shoot a villager, you will die. If the wolves reach parity with the village, you lose.\n")
			} else {
				rolePm.append("If you get shot, you will die. If all wolves get shot, you lose.\n")
				rolePm.append(wolfTeam)
			}
			queue!!.add(toDm(setup.userId, rolePm.toString()))
			playerNumber++
		}

		// Announce game start
		val wolfCount = wolves.size
		queue!!.add(
			toGameChannel(
				String.format(
					"Game has started!\n%s\n**%d** wolves are alive!",
					PopcornHelper.livingPlayersDisplay(world), wolfCount
				)
			)
		)

		// Initiate gun distribution
		if (gameEvent.mode == GameMode.WILD) {
			// WILD mode: random villager gets the gun
			val villagers = PopcornHelper.livingVillagers(world)
			if (!villagers.isEmpty()) {
				val randomVillager: GameEntity =
					villagers.first() // deterministic in tests, caller controls order TODO WTF!!
				randomVillager.addComponent(GunHolderComponent())
				val pc = randomVillager.findComponent<PlayerComponent>()
				val recipientId = pc?.userId() ?: 0

				queue.add(toGameChannel("Randing the gun"))
				queue.add(
					toGameChannel(
						String.format("%s has received the gun!", PopcornHelper.mention(recipientId))
					)
				)

				// Start first day
				val phase = gameState.findComponent<PhaseComponent>()
				phase!!.startNewDay()
				queue.add(
					toGameChannel(
						String.format(
							"Day %d started! %s, you have %d minutes to shoot someone.",
							phase.day(), PopcornHelper.mention(recipientId), phase.dayLengthMillis() / 60000
						)
					)
				)

				// Request day timer
				gameState.addComponent(
					TimerRequestComponent(
						Duration.ofMillis(phase.dayLengthMillis()),
						TimerExpiredEvent(phase.day())
					)
				)
			}
		} else {
			// CLASSIC mode: wolves vote on who gets the gun
			gameState.addComponent(GunDistributionComponent())
			queue.add(toGameChannel("Wolves are distributing the gun! Please stand by."))

			// Request gun distribution timer (1 minute)
			gameState.addComponent(
				TimerRequestComponent(
					Duration.ofSeconds(60),
					GunDistTimerExpiredEvent()
				)
			)
		}
	}
}
