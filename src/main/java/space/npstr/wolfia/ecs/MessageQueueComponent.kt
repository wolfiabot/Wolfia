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
 * Collect output messages from systems. Drained by [MessageFlushSystem] at the end of each dispatch.
 */
class MessageQueueComponent : GameComponent {

	private val messages = mutableListOf<OutputMessage>()

	fun add(message: OutputMessage) {
		messages.add(message)
	}

	fun drain(): List<OutputMessage> {
		val drained = messages.toList()
		messages.clear()
		return drained
	}

	fun peek(): List<OutputMessage> {
		return messages.toList()
	}
}
