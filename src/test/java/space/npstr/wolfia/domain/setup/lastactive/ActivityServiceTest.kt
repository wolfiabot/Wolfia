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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import space.npstr.wolfia.ApplicationTest
import space.npstr.wolfia.TestUtil.sleep
import space.npstr.wolfia.TestUtil.uniqueLong

internal class ActivityServiceTest : ApplicationTest() {

	@Autowired
	private lateinit var service: ActivityService

	@Autowired
	private lateinit var repository: LastActiveRepository

	@Test
	fun whenActivityRecorded_isActiveRecentlyFromBuffer() {
		val userId = uniqueLong()

		service.recordActivity(userId)

		assertThat(service.wasActiveRecently(userId)).isTrue
	}

	@Test
	fun whenActivityRecorded_isNotInDbBeforeFlush() {
		val userId = uniqueLong()

		service.recordActivity(userId)

		assertThat(repository.wasActiveRecently(userId)).isFalse
	}

	@Test
	fun whenFlushed_isInDb() {
		val userId = uniqueLong()
		service.recordActivity(userId)

		service.flush()

		assertThat(repository.wasActiveRecently(userId)).isTrue
	}

	@Test
	fun whenMultipleUsersRecorded_allFlushedToDb() {
		val userIdA = uniqueLong()
		val userIdB = uniqueLong()
		val userIdC = uniqueLong()
		service.recordActivity(userIdA)
		service.recordActivity(userIdB)
		service.recordActivity(userIdC)

		service.flush()

		assertThat(repository.wasActiveRecently(userIdA)).isTrue
		assertThat(repository.wasActiveRecently(userIdB)).isTrue
		assertThat(repository.wasActiveRecently(userIdC)).isTrue
	}

	@Test
	fun whenFlushedWithExpiredTimeout_isNotActive() {
		val userId = uniqueLong()
		repository.recordActivity(userId, Duration.ofMillis(50))
		sleep(Duration.ofMillis(60))

		assertThat(service.wasActiveRecently(userId)).isFalse
	}
}
