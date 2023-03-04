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

import io.lettuce.core.SetArgs
import org.springframework.stereotype.Repository
import space.npstr.wolfia.system.redis.Redis
import java.time.Clock
import java.time.Duration

@Repository
class LastActiveRepository(
    private val redis: Redis,
    private val clock: Clock,
) {

    private val redisKeyParser = RedisKeyParser()

    fun recordActivity(userId: Long, timeout: Duration) {
        val value = clock.millis().toString()
        redis.connection.sync()
            .set(redisKeyParser.toKey(userId), value, SetArgs.Builder.px(timeout.toMillis()))
    }

    fun wasActiveRecently(userId: Long): Boolean {
        return redis.connection.sync()
            .exists(redisKeyParser.toKey(userId)) != 0L
    }
}
