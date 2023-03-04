/*
 * Copyright (C) 2016-2023 the original author or authors
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
import org.springframework.stereotype.Service
import space.npstr.wolfia.config.properties.WolfiaConfig
import java.time.Duration

@Service
class ActivityService(
    private val repository: LastActiveRepository,
    private val wolfiaConfig: WolfiaConfig,
) {

    companion object {
        private val DEFAULT_ACTIVITY_TIMEOUT = Duration.ofMinutes(20)
    }

    fun recordActivity(user: User) {
        recordActivity(user.idLong)
    }

    fun recordActivity(userId: Long) {
        val activityTimeout = if (wolfiaConfig.isDebug) {
            Duration.ofSeconds(30)
        } else {
            DEFAULT_ACTIVITY_TIMEOUT
        }
        repository.recordActivity(userId, activityTimeout)
    }

    fun wasActiveRecently(user: User): Boolean {
        return wasActiveRecently(user.idLong)
    }

    fun wasActiveRecently(userId: Long): Boolean {
        return repository.wasActiveRecently(userId)
    }
}
