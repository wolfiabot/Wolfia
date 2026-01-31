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
package space.npstr.wolfia.domain.oauth2

import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.EnumSet
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.springframework.beans.factory.annotation.Autowired
import space.npstr.wolfia.ApplicationTest
import space.npstr.wolfia.TestUtil
import space.npstr.wolfia.db.type.OAuth2Scope

internal class OAuth2ServiceTest : ApplicationTest() {

	@Autowired
	private lateinit var service: OAuth2Service

	@Autowired
	private lateinit var repository: OAuth2Repository

	@Test
	fun whenAcceptCode_requestFromDiscordAndSave() {
		val userId = TestUtil.uniqueLong()
		val accessToken = "foo"
		val expires = OffsetDateTime.now().plusDays(14).toInstant()
		val refreshToken = "bar"
		val scopes: Set<OAuth2Scope> = EnumSet.allOf(OAuth2Scope::class.java)
		val codeResponse = AccessTokenResponse(accessToken, expires, refreshToken, scopes)

		oAuth2Requester.stub {
			on { fetchCodeResponse(any()) } doReturn codeResponse
			on { identifyUser(any()) } doReturn userId
		}

		runBlocking { service.acceptCode("foo") }

		val oAuth2Data = repository.findOne(userId)!!
		assertThat(oAuth2Data.userId()).isEqualTo(userId)
		assertThat(oAuth2Data.accessToken()).isEqualTo(accessToken)
		assertThat(oAuth2Data.expires()).isCloseTo(expires, within(1, ChronoUnit.MILLIS))
		assertThat(oAuth2Data.refreshToken()).isEqualTo(refreshToken)
		assertThat(oAuth2Data.scopes()).containsExactlyInAnyOrderElementsOf(scopes)
	}

	@Test
	fun givenNoAccessToken_whenGetAccessTokenForScope_returnEmpty() {
		val fetched = service.getAccessTokenForScope(TestUtil.uniqueLong(), OAuth2Scope.IDENTIFY)
		assertThat(fetched).isNull()
	}

	@Test
	fun givenAccessTokenExists_whenGetAccessTokenForScope_returnAccessToken() {
		val userId = TestUtil.uniqueLong()
		val accessToken = "foo"
		val validOAuth2Data = OAuth2Data(
			userId, accessToken, OffsetDateTime.now().plusDays(14).toInstant(),
			"bar", EnumSet.allOf(OAuth2Scope::class.java), Instant.now()
		)
		repository.save(validOAuth2Data)
		val fetched = service.getAccessTokenForScope(userId, OAuth2Scope.IDENTIFY)
		assertThat(fetched).isEqualTo(accessToken)
	}

	@Test
	fun givenAccessTokenExistsWithWrongScope_whenGetAccessTokenForScope_returnEmpty() {
		val userId = TestUtil.uniqueLong()
		val accessToken = "foo"
		val validOAuth2Data = OAuth2Data(
			userId, accessToken, OffsetDateTime.now().plusDays(14).toInstant(),
			"bar", EnumSet.of(OAuth2Scope.IDENTIFY), Instant.now()
		)
		repository.save(validOAuth2Data)
		val fetched = service.getAccessTokenForScope(userId, OAuth2Scope.GUILD_JOIN)
		assertThat(fetched).isNull()
	}

	@Test
	fun givenAccessTokenExistsOutdated_whenGetAccessTokenForScope_returnEmpty() {
		val userId = TestUtil.uniqueLong()
		val accessToken = "foo"
		val validOAuth2Data = OAuth2Data(
			userId, accessToken, OffsetDateTime.now().minusDays(1).toInstant(),
			"bar", EnumSet.allOf(OAuth2Scope::class.java), Instant.now()
		)
		repository.save(validOAuth2Data)
		val fetched = service.getAccessTokenForScope(userId, OAuth2Scope.IDENTIFY)
		assertThat(fetched).isNull()
	}
}
