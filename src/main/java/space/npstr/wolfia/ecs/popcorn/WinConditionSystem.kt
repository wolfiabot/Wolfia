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

import space.npstr.wolfia.ecs.GameEvent
import space.npstr.wolfia.ecs.GameSystem
import space.npstr.wolfia.ecs.GameWorld
import space.npstr.wolfia.ecs.OutputMessage.Companion.toGameChannel
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameMetaComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameOverComponent
import space.npstr.wolfia.game.definitions.Alignments

/**
 * Checks win conditions after every event.
 * Village wins when all wolves are dead. Wolves win when they reach parity with the village.
 */
class WinConditionSystem : GameSystem {

	override fun process(gameEvent: GameEvent, world: GameWorld) {
		if (!PopcornHelper.isGameRunning(world)) return

		val winner = PopcornHelper.checkWinCondition(world)
			?: return

		val gameState = PopcornHelper.findGameStateEntity(world)
			?: return

		val meta = gameState.findComponent<GameMetaComponent>()
		meta?.stop()

		gameState.addComponent(GameOverComponent(winner))

		val queue = PopcornHelper.messageQueue(world)
			?: return

		if (winner == Alignments.VILLAGE) {
			queue.add(toGameChannel("Game over! The " + Alignments.VILLAGE.textRepWW + " wins!"))
		} else {
			queue.add(toGameChannel("Game over! The " + Alignments.WOLF.textRepWW + " win!"))
		}
	}
}
