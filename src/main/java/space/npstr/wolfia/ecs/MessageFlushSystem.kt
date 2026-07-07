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

/**
 * Always registered last. Drains the [MessageQueueComponent] and passes messages to the [OutputSink].
 */
class MessageFlushSystem(
	private val sink: OutputSink,
) : GameSystem {

	override fun process(gameEvent: GameEvent, world: GameWorld) {

		for (entity in world.findEntitiesWith<MessageQueueComponent>()) {
			val queue = entity.findComponent<MessageQueueComponent>()
			if (queue != null) {
				val messages = queue.drain()
				if (!messages.isEmpty()) {
					// Resolve targetChannelId=0 to the game world's channel
					val resolved = messages.stream()
						.map<OutputMessage> { m: OutputMessage ->
							if (m.targetChannelId == 0L && !m.isDm)
								OutputMessage(world.channelId, m.targetUserId, m.content, m.isDm)
							else
								m
						}
						.toList()
					sink.send(resolved)
				}
			}
		}
	}
}
