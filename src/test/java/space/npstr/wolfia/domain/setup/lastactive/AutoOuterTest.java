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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_SECOND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class AutoOuterTest extends ApplicationTest {

    @Autowired
    private LastActiveRepository lastActiveRepository;

    @Test
    void whenUserActivityTimeouts_outUser() {
        long userId = uniqueLong();
        lastActiveRepository.recordActivity(userId, ONE_HUNDRED_MILLISECONDS)
                .toCompletableFuture().join();

        // There is a small chance that this verification might fail, because Redis is not exact when expiring keys
        // See
        // https://redis.io/commands/expire#how-redis-expires-keys
        // and
        // https://redis.io/topics/notifications#timing-of-expired-events
        // Chances are good that this key will be expired though, because redis checks 20 random TTLd keys 10 times per
        // second, and as of writing this, our tests do not create many redis keys.
        verify(gameSetupService, timeout(ONE_SECOND.toMillis())).outUserDueToInactivity(eq(userId), any());
    }

}
