/*
 * Copyright (C) 2016-2020 the original author or authors
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

package space.npstr.wolfia.domain.setup.lastactive;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import javax.annotation.CheckReturnValue;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.system.redis.Redis;

import static io.lettuce.core.SetArgs.Builder.px;

@Repository
public class LastActiveRepository {

    private final RedisKeyParser redisKeyParser = new RedisKeyParser();
    private final Redis redis;
    private final Clock clock;

    public LastActiveRepository(Redis redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @CheckReturnValue
    public CompletionStage<Void> recordActivity(long userId, Duration timeout) {
        String value = Long.toString(this.clock.millis());
        return this.redis.getConnection().async()
                .set(this.redisKeyParser.toKey(userId), value, px(timeout.toMillis()))
                .thenApply(response -> null);
    }

    @CheckReturnValue
    public CompletionStage<Boolean> wasActiveRecently(long userId) {
        return this.redis.getConnection().async()
                .exists(this.redisKeyParser.toKey(userId))
                .thenApply(response -> response != 0);
    }
}
