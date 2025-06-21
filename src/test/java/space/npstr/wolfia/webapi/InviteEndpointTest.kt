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
package space.npstr.wolfia.webapi

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.http.HttpStatus
import space.npstr.wolfia.App
import space.npstr.wolfia.ApplicationTest
import space.npstr.wolfia.DiscordApiConfig
import space.npstr.wolfia.TestUtil

internal class InviteEndpointTest : ApplicationTest() {

	@Test
	fun whenGet_redirects() {
		val response = restTemplate.getForEntity<String>("/invite")

		assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
		assertThat(response.headers.location).satisfies(
			{ uri ->
				assertThat(uri).isNotNull
				assertThat(uri.scheme).isEqualTo("https")
				assertThat(uri.host).isEqualTo("discord.com")
				val httpUrl = uri.toString().toHttpUrl()
				assertThat(httpUrl).isNotNull
				assertThat(httpUrl.pathSegments).containsExactly("oauth2", "authorize")
				assertThat(httpUrl.queryParameter("client_id")).isEqualTo(DiscordApiConfig.SELF_ID.toString())
				assertThat(httpUrl.queryParameter("scope")).isEqualTo("bot applications.commands")
				assertThat(httpUrl.queryParameter("permissions")).isEqualTo("268787777")
				assertThat(httpUrl.queryParameter("response_type")).isEqualTo("code")
				assertThat(httpUrl.queryParameter("redirect_uri")).isEqualTo(App.WOLFIA_LOUNGE_INVITE)
			},
		)
	}

	@Test
	fun whenGetWithGuildId_redirectsWithGuildId() {
		val guildId = TestUtil.uniqueLong()

		val response = restTemplate.getForEntity<String>("/invite?guild_id={guildId}", guildId)

		assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
		assertThat(response.headers.location).satisfies(
			{ uri ->
				assertThat(uri).isNotNull
				val httpUrl = uri.toString().toHttpUrl()
				assertThat(httpUrl.queryParameter("guild_id")).isEqualTo(guildId.toString())
			},
		)
	}

	@Test
	fun whenGetWithCustomRedirectUri_redirectHasRedirectToCustomUri() {
		val redirectUrl = "https://example.org/foo?bar=baz"

		val response = restTemplate.getForEntity<String>("/invite?redirect_uri={redirectUri}", redirectUrl)

		assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
		assertThat(response.headers.location).satisfies(
			{ uri ->
				assertThat(uri).isNotNull
				val httpUrl = uri.toString().toHttpUrl()
				assertThat(httpUrl.queryParameter("redirect_uri")).isEqualTo(redirectUrl)
			},
		)
	}
}
