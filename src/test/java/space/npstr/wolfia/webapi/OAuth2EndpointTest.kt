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
package space.npstr.wolfia.webapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.stub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.util.UriComponentsBuilder
import space.npstr.wolfia.ApplicationTest
import space.npstr.wolfia.TestUtil
import space.npstr.wolfia.db.type.OAuth2Scope
import space.npstr.wolfia.domain.oauth2.AccessTokenResponse
import space.npstr.wolfia.domain.oauth2.AuthState
import space.npstr.wolfia.domain.oauth2.AuthStateCache
import space.npstr.wolfia.domain.oauth2.DiscordRequestFailedException
import java.net.URI
import java.time.OffsetDateTime
import java.util.EnumSet

internal class OAuth2EndpointTest : ApplicationTest() {

    companion object {
        private const val CODE_GRANT_PATH = "/" + OAuth2Endpoint.CODE_GRANT_PATH
        private const val REDIRECT_URL = "https://example.org"
        private const val ACCESS_TOKEN = "42"
        private const val CODE = "69"
    }


    @Autowired
    private lateinit var stateCache: AuthStateCache

    @BeforeEach
    fun setup() {
        val accessTokenResponse = accessTokenResponse()
        oAuth2Requester.stub {
            onBlocking { fetchCodeResponse(eq(CODE)) } doReturn accessTokenResponse
        }
    }

    //ensure that this endpoint is accessible
    @Test
    fun whenGet_andSuccessful_redirect() {
        val userId = TestUtil.uniqueLong()

        oAuth2Requester.stub {
            onBlocking { identifyUser(eq(ACCESS_TOKEN)) } doReturn userId
        }

        val authState = authState(userId)
        val stateParam = stateCache.generateStateParam(authState)

        val uri = UriComponentsBuilder.fromPath(CODE_GRANT_PATH)
            .queryParam("code", CODE)
            .queryParam("state", stateParam)
            .build().toUri()

        val responseEntity = restTemplate.getForEntity(uri, String::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(responseEntity.headers.location).isEqualTo(URI.create(REDIRECT_URL))
    }

    @Test
    fun whenGet_noCodeParam_return400() {
        val authState = authState(TestUtil.uniqueLong())
        val stateParam = stateCache.generateStateParam(authState)

        val uri = UriComponentsBuilder.fromPath(CODE_GRANT_PATH)
            .queryParam("state", stateParam)
            .build().toUri()

        val responseEntity = restTemplate.getForEntity(uri, String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun whenGet_noStateParam_return400() {
        val uri = UriComponentsBuilder.fromPath(CODE_GRANT_PATH)
            .queryParam("code", CODE)
            .build().toUri()

        val responseEntity = restTemplate.getForEntity(uri, String::class.java)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(responseEntity.body).contains(OAuth2Endpoint.GENERIC_ERROR_RESPONSE)
    }

    @Test
    fun whenGet_noCachedState_return400() {
        val userId = TestUtil.uniqueLong()

        oAuth2Requester.stub {
            onBlocking { identifyUser(eq(ACCESS_TOKEN)) } doReturn userId
        }

        val uri = UriComponentsBuilder.fromPath(CODE_GRANT_PATH)
            .queryParam("code", CODE)
            .queryParam("state", "42")
            .build().toUri()

        val responseEntity = restTemplate.getForEntity(uri, String::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(responseEntity.body).contains(OAuth2Endpoint.GENERIC_ERROR_RESPONSE)
    }

    @Test
    fun whenGet_differentUserId_return400() {
        val userIdA = TestUtil.uniqueLong()
        val userIdB = TestUtil.uniqueLong()

        oAuth2Requester.stub {
            onBlocking { identifyUser(eq(ACCESS_TOKEN)) } doReturn userIdA
        }

        val authState = authState(userIdB)
        val stateParam = stateCache.generateStateParam(authState)


        val uri = UriComponentsBuilder.fromPath(CODE_GRANT_PATH)
            .queryParam("code", CODE)
            .queryParam("state", stateParam)
            .build().toUri()

        val responseEntity = restTemplate.getForEntity(uri, String::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(responseEntity.body).contains(OAuth2Endpoint.WRONG_ACCOUNT_RESPONSE)
    }

    @Test
    fun whenGet_identificationFails_return400() {
        oAuth2Requester.stub {
            onBlocking { identifyUser(eq(ACCESS_TOKEN)) } doThrow DiscordRequestFailedException("lol nope")
        }

        val authState = authState(TestUtil.uniqueLong())
        val stateParam = stateCache.generateStateParam(authState)

        val uri = UriComponentsBuilder.fromPath(CODE_GRANT_PATH)
            .queryParam("code", CODE)
            .queryParam("state", stateParam)
            .build().toUri()

        val responseEntity = restTemplate.getForEntity(uri, String::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(responseEntity.body).contains(OAuth2Endpoint.DISCORD_ISSUES)
    }

    @Test
    fun whenGet_randomException_return500() {
        oAuth2Requester.stub {
            onBlocking { identifyUser(eq(ACCESS_TOKEN)) } doThrow RuntimeException("lol nope")
        }

        val authState = authState(TestUtil.uniqueLong())
        val stateParam = stateCache.generateStateParam(authState)

        val uri = UriComponentsBuilder.fromPath(CODE_GRANT_PATH)
            .queryParam("code", CODE)
            .queryParam("state", stateParam)
            .build().toUri()

        val responseEntity = restTemplate.getForEntity(uri, String::class.java)
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(responseEntity.body).contains(OAuth2Endpoint.GENERIC_ERROR_RESPONSE)
    }

    private fun accessTokenResponse(): AccessTokenResponse {
        return AccessTokenResponse(
            ACCESS_TOKEN, OffsetDateTime.now().plusMonths(1).toInstant(),
            ACCESS_TOKEN, EnumSet.allOf(OAuth2Scope::class.java)
        )
    }

    private fun authState(userId: Long): AuthState {
        return AuthState(userId, REDIRECT_URL)
    }
}
