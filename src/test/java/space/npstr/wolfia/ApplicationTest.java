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

package space.npstr.wolfia;

import io.prometheus.client.CollectorRegistry;
import java.time.Clock;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import space.npstr.wolfia.domain.UserCache;
import space.npstr.wolfia.domain.oauth2.OAuth2Requester;
import space.npstr.wolfia.domain.privacy.PrivacyBanService;
import space.npstr.wolfia.domain.privacy.PrivacyCommand;
import space.npstr.wolfia.domain.setup.GameSetupService;
import space.npstr.wolfia.domain.stats.StatsService;

/**
 * Extend this class from tests that require a Spring Application Context
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.config.name=wolfia")
@AutoConfigureMockMvc
public abstract class ApplicationTest extends PostgresAndRedisContainers {

    @LocalServerPort
    protected int port;

    @SpyBean
    protected Clock clock;

    @SpyBean
    protected GameSetupService gameSetupService;

    @SpyBean
    protected UserCache userCache;

    @SpyBean
    protected PrivacyCommand privacyCommand;

    @SpyBean
    protected StatsService statsService;

    @SpyBean
    protected PrivacyBanService privacyBanService;

    @MockBean
    protected OAuth2Requester oAuth2Requester;

    @Autowired // is actually a mock, see DiscordApiConfig
    protected ShardManager shardManager;

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Some static metrics are giving trouble when the application context is restarted between tests.
     */
    @AfterAll
    static void clearCollectorRegistry() {
        CollectorRegistry.defaultRegistry.clear();
    }

}
