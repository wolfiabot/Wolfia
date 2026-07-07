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

import java.util.function.Consumer
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import space.npstr.wolfia.ecs.OutputMessage
import space.npstr.wolfia.ecs.OutputSink
import space.npstr.wolfia.system.logger
import space.npstr.wolfia.utils.discord.RestActions

/**
 * Translates ECS OutputMessages into JDA RestActions calls.
 *
 * TODO this needs some careful firewalling against JDAs crap interfacing.
 */
@Component
class DiscordOutputAdapter(
	private val shardManager: ShardManager,
) : OutputSink {

	override fun send(messages: List<OutputMessage>) {
		for (message in messages) {
			try {
				if (message.isDm) {
					sendDm(message)
				} else {
					sendToChannel(message)
				}
			} catch (e: Exception) {
				logger().error("Failed to send output message: {}", message, e)
			}
		}
	}

	private fun sendToChannel(message: OutputMessage) {
		val channelId = message.targetChannelId
		val channel: MessageChannel? = shardManager.getTextChannelById(channelId)
		if (channel == null) {
			logger().warn("Could not find channel {} to send message", channelId)
			return
		}
		RestActions.sendMessage(channel, message.content)
	}

	private fun sendDm(message: OutputMessage) {
		val user = shardManager.getUserById(message.targetUserId)
		if (user == null) {
			// Try to retrieve the user
			shardManager.retrieveUserById(message.targetUserId).queue(
				Consumer { u: User? ->
					RestActions.sendPrivateMessage(
						u, message.content, null
					) { t -> logger().warn("Could not DM user {}", message.targetUserId, t) }
				},
				Consumer { t: Throwable? ->
					logger().warn(
						"Could not retrieve user {} for DM",
						message.targetUserId,
						t
					)
				}
			)
			return
		}
		RestActions.sendPrivateMessage(
			user, message.content, null
		) { t -> logger().warn("Could not DM user {}", message.targetUserId, t) }
	}
}
