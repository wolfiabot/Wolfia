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

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.SECONDS
import net.dv8tion.jda.api.entities.User
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.npstr.wolfia.config.properties.WolfiaConfig
import space.npstr.wolfia.system.logger

@Service
class ActivityService(
	private val repository: LastActiveRepository,
	private val wolfiaConfig: WolfiaConfig,
) {

	companion object {
		private val DEFAULT_ACTIVITY_TIMEOUT = Duration.ofMinutes(20)
	}

	private val buffer: MutableSet<Long> = ConcurrentHashMap.newKeySet()

	private val activityTimeout: Duration
		get() = if (wolfiaConfig.isDebug) Duration.ofSeconds(30) else DEFAULT_ACTIVITY_TIMEOUT

	fun recordActivity(user: User) {
		recordActivity(user.idLong)
	}

	fun recordActivity(userId: Long) {
		buffer.add(userId)
	}

	@Scheduled(fixedDelay = 1, timeUnit = SECONDS, initialDelay = 1)
	internal fun flush() {
		if (buffer.isEmpty()) return

		val snapshot = HashSet(buffer)
		buffer.removeAll(snapshot)

		try {
			repository.recordActivities(snapshot, activityTimeout)
		} catch (e: Exception) {
			logger().warn("Failed to flush {} activity records", snapshot.size, e)
		}
	}

	fun wasActiveRecently(user: User): Boolean {
		return wasActiveRecently(user.idLong)
	}

	fun wasActiveRecently(userId: Long): Boolean {
		if (buffer.contains(userId)) {
			return true
		}
		return repository.wasActiveRecently(userId)
	}
}
