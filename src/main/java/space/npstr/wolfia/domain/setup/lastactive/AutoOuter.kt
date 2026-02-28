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

import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.npstr.wolfia.domain.setup.GameSetupService

/**
 * This component listens the keys expiring from [ActivityService] and takes action
 */
@Component
class AutoOuter(
	private val gameSetupService: GameSetupService,
	private val shardManager: ShardManager,
) {

	@EventListener
	fun onUserBecameInactive(event: LastActiveRepository.UserBecameInactive) {
		outUser(event.userId)
	}

	private fun outUser(userId: Long) {
		gameSetupService.outUserDueToInactivity(userId, this.shardManager)
	}
}
