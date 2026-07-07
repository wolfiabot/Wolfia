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

import space.npstr.wolfia.ecs.GameEntity
import space.npstr.wolfia.ecs.GameWorld
import space.npstr.wolfia.ecs.MessageQueueComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameMetaComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunHolderComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PlayerComponent
import space.npstr.wolfia.game.definitions.Alignments

/**
 * Shared query helpers for Popcorn systems.
 */
internal object PopcornHelper {

	fun findGameStateEntity(world: GameWorld): GameEntity? {
		return world.findEntitiesWith<GameMetaComponent>().firstOrNull()
	}

	fun messageQueue(world: GameWorld): MessageQueueComponent? {
		return findGameStateEntity(world)?.findComponent<MessageQueueComponent>()
	}

	fun findPlayerEntity(world: GameWorld, userId: Long): GameEntity? {
		for (entity in world.findEntitiesWith<PlayerComponent>()) {
			val pc = entity.findComponent<PlayerComponent>()
			if (pc?.userId() == userId) {
				return entity
			}
		}
		return null
	}

	fun livingPlayers(world: GameWorld): List<GameEntity> {
		return world.findEntitiesWith<PlayerComponent>()
			.filter { it.findComponent<PlayerComponent>()?.isAlive ?: false }
	}

	fun livingWolves(world: GameWorld): List<GameEntity> {
		return livingPlayers(world)
			.filter { it.findComponent<PlayerComponent>()?.isWolf ?: false }
	}

	fun livingVillagers(world: GameWorld): List<GameEntity> {
		return livingPlayers(world)
			.filter { it.findComponent<PlayerComponent>()?.isVillage ?: false }
	}

	fun gunHolder(world: GameWorld): GameEntity? {
		return world.findEntitiesWith<GunHolderComponent>().firstOrNull()
	}

	fun mention(userId: Long): String {
		return "<@$userId>"
	}

	fun isGameRunning(world: GameWorld): Boolean {
		return findGameStateEntity(world)
			?.findComponent<GameMetaComponent>()
			?.isRunning
			?: false
	}

	fun countLivingWolves(world: GameWorld): Int {
		return livingWolves(world).size
	}

	fun countLivingVillagers(world: GameWorld): Int {
		return livingVillagers(world).size
	}

	fun isLivingWolf(world: GameWorld, userId: Long): Boolean {
		return findPlayerEntity(world, userId)
			?.findComponent<PlayerComponent>()
			?.let { it.isAlive && it.isWolf }
			?: false
	}

	fun livingVillagerIds(world: GameWorld): List<Long> {
		return livingVillagers(world)
			.mapNotNull { it.findComponent<PlayerComponent>() }
			.map { it.userId() }
	}

	fun livingPlayersDisplay(world: GameWorld): String {
		val sb = StringBuilder()
		for (entity in livingPlayers(world)) {
			val pc = entity.findComponent<PlayerComponent>()
			if (pc != null) {
				sb.append(mention(pc.userId())).append(" ")
			}
		}
		return sb.toString().trim()
	}

	fun checkWinCondition(world: GameWorld): Alignments? {
		val wolves = countLivingWolves(world)
		val villagers = countLivingVillagers(world)

		if (wolves == 0) return Alignments.VILLAGE
		if (wolves >= villagers) return Alignments.WOLF
		return null
	}
}
