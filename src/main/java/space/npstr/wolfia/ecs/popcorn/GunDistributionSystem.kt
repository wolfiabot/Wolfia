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
import space.npstr.wolfia.ecs.OutputMessage.Companion.toGameChannel
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunDistributionComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunHolderComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PhaseComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.TimerRequestComponent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GunDistTimerExpiredEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GunDistVoteEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.TimerExpiredEvent

/**
 * Handles gun distribution in CLASSIC mode. Wolves vote on which villager gets the gun.
 * Distribution ends when all living wolves have voted or the timer expires.
 */
class GunDistributionSystem : GameSystem {
	override fun process(gameEvent: GameEvent, world: GameWorld) {
		if (!PopcornHelper.isGameRunning(world)) return

		if (gameEvent is GunDistVoteEvent) {
			handleVote(gameEvent, world)
		} else if (gameEvent is GunDistTimerExpiredEvent) {
			handleTimerExpired(world)
		}
	}

	private fun handleVote(vote: GunDistVoteEvent, world: GameWorld) {
		val gameState = PopcornHelper.findGameStateEntity(world)
			?: return
		val dist = gameState.findComponent<GunDistributionComponent>()
		if (dist == null || dist.isDone) return

		// Only living wolves may vote
		if (!PopcornHelper.isLivingWolf(world, vote.voterId)) return

		// Candidate must be a living villager
		val villagerIds = PopcornHelper.livingVillagerIds(world)
		if (!villagerIds.contains(vote.candidateId)) return

		dist.vote(vote.voterId, vote.candidateId)

		// Check if all living wolves have voted
		val livingWolfCount = PopcornHelper.livingWolves(world).size
		if (dist.votes().size >= livingWolfCount) {
			resolveDistribution(world, gameState, dist)
		}
	}

	private fun handleTimerExpired(world: GameWorld) {
		val gameState = PopcornHelper.findGameStateEntity(world)
			?: return
		val dist = gameState.findComponent<GunDistributionComponent>()
		if (dist == null || dist.isDone) return

		resolveDistribution(world, gameState, dist)
	}

	private fun resolveDistribution(world: GameWorld, gameState: GameEntity, dist: GunDistributionComponent) {
		dist.markDone()
		gameState.removeComponents<GunDistributionComponent>()

		val queue = PopcornHelper.messageQueue(world)
			?: return

		val villagerIds = PopcornHelper.livingVillagerIds(world)
		val recipientId = mostVotedOrFirst(dist.votes(), villagerIds)

		val recipientEntity = PopcornHelper.findPlayerEntity(world, recipientId)
			?: return

		recipientEntity.addComponent(GunHolderComponent())
		queue.add(
			toGameChannel(
				String.format("%s has received the gun!", PopcornHelper.mention(recipientId))
			)
		)

		// Start the day
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

		gameState.addComponent(
			TimerRequestComponent(
				Duration.ofMillis(phase.dayLengthMillis()),
				TimerExpiredEvent(phase.day())
			)
		)
	}

	companion object {
		/**
		 * Find the most voted candidate. Ties are broken by picking the first candidate in the villager list.
		 * If no votes were cast, pick the first villager.
		 */
		fun mostVotedOrFirst(votes: Map<Long, Long>, allCandidates: List<Long>): Long {
			if (allCandidates.isEmpty()) return -1
			if (votes.isEmpty()) return allCandidates.first()

			// Count votes per candidate
			val voteCounts = mutableMapOf<Long, Long>()
			for (candidateId in allCandidates.toSet()) {
				val count = votes.values.count { candidateId == it }
				voteCounts[candidateId] = count.toLong()
			}

			val maxVotes = voteCounts.values.maxOrNull() ?: 0
			if (maxVotes == 0L) return allCandidates.first()

			// Pick the first candidate (by allCandidates order) with max votes
			for (candidateId in allCandidates) {
				if (voteCounts.getOrDefault(candidateId, 0L) == maxVotes) {
					return candidateId
				}
			}
			return allCandidates.first()
		}
	}
}
