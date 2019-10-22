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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;

/**
 * Extend this class from tests that require a Spring Application Context
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.config.name=wolfia")
public abstract class ApplicationTest extends PostgresContainer {

    @SpyBean
    protected Clock clock;

    /**
     * Some static metrics are giving trouble when the application context is restarted between tests.
     */
    @AfterAll
    static void clearCollectorRegistry() {
        CollectorRegistry.defaultRegistry.clear();
    }

}
