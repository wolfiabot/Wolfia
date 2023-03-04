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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import space.npstr.wolfia.ApplicationTest
import space.npstr.wolfia.TestUtil
import java.time.Duration

internal class LastActiveRepositoryTest : ApplicationTest() {

    @Autowired
    private lateinit var repository: LastActiveRepository

    @Test
    fun whenNotActive_returnFalse() {
        val active = repository.wasActiveRecently(TestUtil.uniqueLong())

        assertThat(active).isFalse
    }

    @Test
    fun whenActive_returnTrue() {
        val userId = TestUtil.uniqueLong()
        repository.recordActivity(userId, Duration.ofHours(1))

        val active = repository.wasActiveRecently(userId)

        assertThat(active).isTrue
    }

    @Test
    fun whenActive_returnFalseAfterTimeout() {
        val userId = TestUtil.uniqueLong()
        repository.recordActivity(userId, Duration.ofMillis(50))
        TestUtil.sleep(Duration.ofMillis(60))

        val active = repository.wasActiveRecently(userId)

        assertThat(active).isFalse
    }
}
