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
 * A message produced by game systems, to be sent by an output adapter.
 *
 * @param targetChannelId the channel to send to (0 for the game channel)
 * @param targetUserId    the user to DM (0 if not a DM)
 * @param content         the message text
 * @param isDm            whether this should be sent as a DM
 */
data class OutputMessage(
	val targetChannelId: Long,
	val targetUserId: Long,
	val content: String,
	val isDm: Boolean,
) {

	companion object {
		fun toChannel(channelId: Long, content: String): OutputMessage {
			return OutputMessage(channelId, 0, content, false)
		}

		fun toGameChannel(content: String): OutputMessage {
			return OutputMessage(0, 0, content, false)
		}

		fun toDm(userId: Long, content: String): OutputMessage {
			return OutputMessage(0, userId, content, true)
		}
	}
}
