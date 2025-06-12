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
package space.npstr.wolfia.domain.setup.lastactive

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.user.UserTypingEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class DiscordActivityListener(
	private val service: ActivityService
) {

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@EventListener
	fun onUserTyping(event: UserTypingEvent) {
		active(event.user)
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@EventListener
	fun onMessageReceived(event: MessageReceivedEvent) {
		active(event.author)
	}

	private fun active(user: User) {
		service.recordActivity(user)
	}
}
