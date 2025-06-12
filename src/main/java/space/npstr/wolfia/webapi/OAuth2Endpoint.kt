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

import java.net.URI
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.npstr.wolfia.config.properties.WolfiaConfig
import space.npstr.wolfia.domain.oauth2.AuthCommand
import space.npstr.wolfia.domain.oauth2.AuthStateCache
import space.npstr.wolfia.domain.oauth2.DiscordRequestFailedException
import space.npstr.wolfia.domain.oauth2.OAuth2Data
import space.npstr.wolfia.domain.oauth2.OAuth2Service
import space.npstr.wolfia.system.logger

@RestController
@RequestMapping("/" + OAuth2Endpoint.CODE_GRANT_PATH)
class OAuth2Endpoint(
	private val service: OAuth2Service,
	private val stateCache: AuthStateCache,
) {

	companion object {
		const val CODE_GRANT_PATH = "public/oauth2/discord"
		const val GENERIC_ERROR_RESPONSE = ("Something was off with your request. Try authorizing again with "
			+ WolfiaConfig.DEFAULT_PREFIX + AuthCommand.TRIGGER)
		const val WRONG_ACCOUNT_RESPONSE = ("It looks like you are logged into a different account in your browser."
			+ " than in your app. Log out in your browser, then say " + WolfiaConfig.DEFAULT_PREFIX + AuthCommand.TRIGGER
			+ " to start authentication again, and then log in into the same account you are using to play Wolfia.")
		const val DISCORD_ISSUES = ("Something went :boom: when talking to Discord. Try authorizing again with "
			+ WolfiaConfig.DEFAULT_PREFIX + AuthCommand.TRIGGER + " or try again later.")
	}


	@GetMapping
	fun codeGrant(@RequestParam("code") code: String, @RequestParam(name = "state", required = false) state: String?): ResponseEntity<String> {
		val authStateOpt = stateCache.getAuthState(state)
		if (authStateOpt.isEmpty) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_ERROR_RESPONSE)
		}
		val (userId, redirectUrl) = authStateOpt.get()
		val data: OAuth2Data = try {
			service.acceptCodeBlocking(code)
		} catch (e: Exception) {
			logger().error("Uncaught exception", e)
			return if (e is DiscordRequestFailedException) {
				ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DISCORD_ISSUES)
			} else {
				ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GENERIC_ERROR_RESPONSE)
			}
		}
		if (data.userId() != userId) {
			logger().info("Flow initiated by user {} was finished by user {}", userId, data.userId())
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WRONG_ACCOUNT_RESPONSE)
		}
		val scopes = data.scopes().joinToString(", ") { it.name }
		logger().info("User {} authorized with scopes {}", data.userId(), scopes)
		val headers = HttpHeaders()
		headers.location = URI.create(redirectUrl)
		return ResponseEntity("", headers, HttpStatus.TEMPORARY_REDIRECT)
	}
}
