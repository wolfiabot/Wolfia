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
package space.npstr.wolfia

import java.time.Clock
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import space.npstr.wolfia.domain.UserCache
import space.npstr.wolfia.domain.oauth2.OAuth2Requester
import space.npstr.wolfia.domain.privacy.PrivacyBanService
import space.npstr.wolfia.domain.privacy.PrivacyCommand
import space.npstr.wolfia.domain.setup.GameSetupService
import space.npstr.wolfia.domain.stats.StatsService

/**
 * Extend this class from tests that require a Spring Application Context
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.config.name=wolfia"])
@AutoConfigureMockMvc
abstract class ApplicationTest : PostgresAndRedisContainers() {

	@LocalServerPort
	protected var port: Int = 0

	@MockitoSpyBean
	protected lateinit var clock: Clock

	@MockitoSpyBean
	protected lateinit var gameSetupService: GameSetupService

	@MockitoSpyBean
	protected lateinit var userCache: UserCache

	@MockitoSpyBean
	protected lateinit var privacyCommand: PrivacyCommand

	@MockitoSpyBean
	protected lateinit var statsService: StatsService

	@MockitoSpyBean
	protected lateinit var privacyBanService: PrivacyBanService

	@MockitoBean
	protected lateinit var oAuth2Requester: OAuth2Requester

	@Autowired // is actually a mock, see DiscordApiConfig
	protected lateinit var shardManager: ShardManager

	@Autowired
	protected lateinit var restTemplate: TestRestTemplate
}
