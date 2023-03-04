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

import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.npstr.wolfia.config.properties.OAuth2Config
import space.npstr.wolfia.system.logger

/**
 * This controller's sole purpose us to redirect to an internal login endpoint that is not exposed under /api
 * (and thus annoying to expose in dev environments to the web frontend server started by yarn).
 */
@RestController
@RequestMapping(LoginRedirect.ROUTE)
class LoginRedirect(
	private val oAuth2Config: OAuth2Config,
) {

	companion object {
		const val INIT_DISCORD_LOGIN = "/oauth2/authorization/discord"
		const val ROUTE = "/public/login"
		const val LOGIN_REDIRECT_SESSION_ATTRIBUTE = "WOLFIA_LOGIN_REDIRECT"
	}

	private val home: URI = URI.create(oAuth2Config.baseRedirectUrl)

	@GetMapping
	fun login(
		@RequestParam(required = false, name = "login_redirect") loginRedirect: String?,
		request: HttpServletRequest,
	): ResponseEntity<Unit> {
		val headers = HttpHeaders()
		val securityContext = SecurityContextHolder.getContext()
		val existingAuth = securityContext.authentication
		val loginRedirectUrl = getLoginRedirectUrl(loginRedirect)
		if (existingAuth != null && existingAuth.isAuthenticated && existingAuth.authorities.contains(Authorization.USER)) {
			headers.location = loginRedirectUrl
		} else {
			request.session.setAttribute(LOGIN_REDIRECT_SESSION_ATTRIBUTE, loginRedirectUrl.toString())
			headers.location = URI.create(INIT_DISCORD_LOGIN)
		}
		return ResponseEntity(null, headers, HttpStatus.TEMPORARY_REDIRECT)
	}

	/**
	 * Note that the URL is fully qualified and has to pass at least Java's URI parser.
	 *
	 * @param loginRedirect a path, not a fully qualified URL. Will be appended to the applications base url.
	 */
	private fun getLoginRedirectUrl(loginRedirect: String?): URI {
		return loginRedirect
			?.let { uri: String ->
				return@let try {
					URI.create(oAuth2Config.baseRedirectUrl + uri)
				} catch (e: Exception) {
					logger().warn("Not a valid URI: {}", uri)
					null
				}
			}
			?: home
	}

	@DeleteMapping
	fun logout(request: HttpServletRequest): ResponseEntity<Void> {
		SecurityContextHolder.clearContext()
		request.getSession(false)?.invalidate()
		return ResponseEntity(HttpStatus.NO_CONTENT)
	}

}
