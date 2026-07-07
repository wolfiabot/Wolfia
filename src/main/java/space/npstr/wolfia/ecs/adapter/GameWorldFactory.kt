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

import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import org.springframework.stereotype.Component
import space.npstr.wolfia.ecs.GameWorld
import space.npstr.wolfia.ecs.MessageFlushSystem
import space.npstr.wolfia.ecs.popcorn.GameStartSystem
import space.npstr.wolfia.ecs.popcorn.GunDistributionSystem
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GameStartEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.PlayerSetup
import space.npstr.wolfia.ecs.popcorn.PopcornHelper.checkWinCondition
import space.npstr.wolfia.ecs.popcorn.ShootSystem
import space.npstr.wolfia.ecs.popcorn.TimerSystem
import space.npstr.wolfia.ecs.popcorn.WinConditionSystem
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.popcorn.PopcornInfo
import space.npstr.wolfia.system.logger

/**
 * Creates GameWorld instances with the correct systems wired up.
 */
@Component
class GameWorldFactory(
	private val outputAdapter: DiscordOutputAdapter,
	private val bridge: EcsGameBridge,
) {

	private val timerScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { r ->
		Thread(r, "ecs-timer-scheduler")
			.also { it.isDaemon = true }
	}

	/**
	 * Creates and starts a Popcorn game via ECS.
	 *
	 * @param channelId  the Discord channel hosting the game
	 * @param guildId    the Discord guild
	 * @param innedUsers the player user IDs
	 * @param mode       WILD or CLASSIC
	 * @param dayLength  how long the gun bearer has to shoot
	 * @return the created GameWorld
	 */
	fun createPopcornGame(
		channelId: Long,
		guildId: Long,
		innedUsers: MutableSet<Long>,
		mode: GameMode,
		dayLength: Duration,
	): GameWorld {
		// Build character setup using existing PopcornInfo logic
		val popcornInfo = PopcornInfo()
		val charakterSetup = popcornInfo.getCharacterSetup(mode, innedUsers.size)

		// Shuffle players for random role assignment
		val shuffledPlayers = innedUsers.toMutableList()
		shuffledPlayers.shuffle()

		// getRandedCharakters() shuffles internally, but we just need the alignment distribution
		val characters = charakterSetup.getRandedCharakters().toMutableList()
		val playerSetups = mutableListOf<PlayerSetup>()
		for (i in shuffledPlayers.indices) {
			playerSetups.add(PlayerSetup(shuffledPlayers[i], characters[i].alignment))
		}

		// Create the world with systems in order
		val world = GameWorld(channelId)
		world.addSystem(GameStartSystem())
		world.addSystem(ShootSystem())
		world.addSystem(GunDistributionSystem())
		world.addSystem(TimerSystem(timerScheduler))
		world.addSystem(WinConditionSystem())
		// Game-over cleanup system: removes the world from the bridge when the game ends
		world.addSystem(GameOverCleanupSystem(bridge))
		world.addSystem(MessageFlushSystem(outputAdapter))

		bridge.register(world)

		// Submit the start event
		world.submit(GameStartEvent(channelId, guildId, playerSetups, mode, dayLength))

		logger().info(
			"ECS Popcorn game started in channel {} with {} players in {} mode",
			channelId, innedUsers.size, mode
		)

		checkWinCondition(world)

		return world
	}

}
