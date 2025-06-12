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
package space.npstr.wolfia.domain.discord

import java.util.Objects
import java.util.function.Supplier
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException.Unauthorized
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException

/**
 * Run our own requests against the Discord Api on behalf of users using their access tokens (OAuth2).
 *
 * TODO we need a proper ratelimiter implementation for discord requests
 */
@Component
class DiscordRequester {

	companion object {
		private const val DISCORD_API_URL = "https://discord.com/api/v6"
	}

	private final val restTemplate: RestTemplate = RestTemplateBuilder()
		.requestFactory(Supplier { JdkClientHttpRequestFactory() })
		.rootUri(DISCORD_API_URL)
		.build()

	fun fetchUser(accessToken: String): PartialUser {
		val headers = HttpHeaders()
		headers["Authorization"] = "Bearer $accessToken"
		val exchange: ResponseEntity<PartialUser> = try {
			restTemplate.exchange(
				"/users/@me",
				HttpMethod.GET,
				HttpEntity<Any>(headers),
				object : ParameterizedTypeReference<PartialUser>() {},
			)
		} catch (e: Unauthorized) {
			throw handleUnauthorized()
		}
		val user = exchange.body
		return Objects.requireNonNull(user, "fetched user is null")
	}

	fun fetchAllGuilds(accessToken: String): List<PartialGuild> {
		val headers = HttpHeaders()
		headers["Authorization"] = "Bearer $accessToken"
		val exchange: ResponseEntity<List<PartialGuild>> = try {
			restTemplate.exchange(
				"/users/@me/guilds",
				HttpMethod.GET,
				HttpEntity<Any>(headers),
				object : ParameterizedTypeReference<List<PartialGuild>>() {},
			)
		} catch (e: Unauthorized) {
			throw handleUnauthorized()
		}
		val guilds = exchange.body
		return Objects.requireNonNull(guilds, "fetched guilds are null")
	}

	private fun handleUnauthorized(): ResponseStatusException {
		val requestAttributes = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
		SecurityContextHolder.clearContext()
		val session = requestAttributes.request.getSession(false)
		session?.invalidate()
		return ResponseStatusException(HttpStatus.UNAUTHORIZED)
	}
}
