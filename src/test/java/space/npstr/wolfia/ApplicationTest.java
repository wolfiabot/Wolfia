/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

package space.npstr.wolfia;

import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extend this class from tests that require a Spring Application Context
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class ApplicationTest extends PostgresContainer {

    private static final AtomicLong LONGS = new AtomicLong(
            ThreadLocalRandom.current().nextLong(Long.MAX_VALUE / 2, Long.MAX_VALUE)
    );

    /**
     * Some static metrics are giving trouble when the application context is restarted between tests.
     */
    @AfterAll
    static void clearCollectorRegistry() {
        CollectorRegistry.defaultRegistry.clear();
    }

    /**
     * This is useful for example to ensure eventual parallel tests don't overwrite database entries with the same ids.
     * <p>
     * This will return at least half of the amount of values in the range between 0 and {@link Long#MAX_VALUE}
     *
     * @return A long value that is unique in the scope of the whole test run.
     */
    protected static long uniqueLong() {
        long value = LONGS.decrementAndGet();
        if (value < 0) {
            throw new RuntimeException("Exhausted unique long values during tests");
        }
        return value;
    }

}
