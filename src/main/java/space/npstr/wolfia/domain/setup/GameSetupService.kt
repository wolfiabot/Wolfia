/*
 * Copyright (C) 2016-2025 the original author or authors
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
package space.npstr.wolfia.domain.setup

import java.time.Duration
import java.util.function.Consumer
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Service
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Games
import space.npstr.wolfia.utils.discord.TextchatUtils

@Service
class GameSetupService private constructor(
	private val repository: GameSetupRepository
) {

	fun outUserDueToInactivity(userId: Long, shardManager: ShardManager) {
		val setups = repository.findAutoOutSetupsWhereUserIsInned(userId)
		for (setup in setups) {
			channel(setup.channelId)
				.outUserDueToInactivity(userId, shardManager)
		}
	}

	/**
	 * This service has many calls that require passing in multiple long ids. This fluent action api should help avoid
	 * mistakes where arguments are passed in the wrong order.
	 *
	 * @return an action that can be executed on the passed in channel
	 */
	fun channel(channelId: Long): Action {
		return Action(channelId)
	}

	inner class Action internal constructor(private val channelId: Long) {

		fun getOrDefault(): GameSetup {
			return repository.findOneOrDefault(channelId)
		}

		fun setGame(game: Games): GameSetup {
			return repository.setGame(channelId, game)
		}

		fun setMode(mode: GameMode): GameSetup {
			return repository.setMode(channelId, mode)
		}

		fun setDayLength(duration: Duration): GameSetup {
			return repository.setDayLength(channelId, duration)
		}

		fun inUser(userId: Long): GameSetup {
			return inUsers(setOf(userId))
		}

		fun inUsers(userIds: Set<Long>): GameSetup {
			return if (userIds.isEmpty()) {
				getOrDefault()
			} else repository.inUsers(channelId, userIds)
		}

		fun outUser(userId: Long): GameSetup {
			return outUsers(setOf(userId))
		}

		fun outUserDueToInactivity(userId: Long, shardManager: ShardManager): GameSetup {
			val setup = getOrDefault()
			if (!setup.innedUsers.contains(userId)) {
				return setup
			}
			val channel = shardManager.getTextChannelById(setup.channelId)
			channel?.sendMessage(
				TextchatUtils.userAsMention(userId)
					+ " became inactive and were outed from the game setup."
			)?.queue()
			return outUser(userId)
		}

		fun outUsers(userIds: Set<Long>): GameSetup {
			return if (userIds.isEmpty()) {
				getOrDefault()
			} else repository.outUsers(channelId, userIds)
		}

		fun clearInnedUsers(): GameSetup {
			return outUsers(getOrDefault().innedUsers)
		}

		fun reset() {
			repository.delete(channelId)
		}

		/**
		 * Like [Action.getOrDefault], but cleans up left/inactive players first if possible.
		 */
		fun cleanUpInnedPlayers(shardManager: ShardManager): GameSetup {
			val setup = getOrDefault()
			val channel = shardManager.getTextChannelById(channelId) ?: return setup
			val toBeOuted: MutableSet<Long> = HashSet()
			val guild = channel.guild
			setup.innedUsers.forEach(Consumer { userId: Long ->
				if (guild.getMemberById(userId) == null) {
					toBeOuted.add(userId)
					channel.sendMessage(
						TextchatUtils.userAsMention(userId)
							+ " has left this guild and was outed from the game setup."
					).queue()
				}
			})
			return outUsers(toBeOuted)
		}
	}
}
