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

import java.util.Base64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import space.npstr.wolfia.ApplicationTest

/**
 * The Togglz Console is served by a Servlet, so we can't use MockMvc.
 */
internal class TogglzEndpointTest<T : Session> : ApplicationTest() {

	private final val togglzConsolePath = "/api/togglz/index"

	@Autowired
	@Suppress("SpringJavaInjectionPointsAutowiringInspection")
	private lateinit var sessionRepository: SessionRepository<T>

	@Test
	fun whenGet_withoutAuthentication_returnUnauthorized() {
		val response = restTemplate.getForEntity("/$togglzConsolePath", Void::class.java)

		assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
	}

	@Test
	fun whenGet_withUserAuthority_returnUnauthorized() {
		val headers = HttpHeaders()
		headers.add(HttpHeaders.COOKIE, sessionCookie(generateHttpSession(Authorization.USER)))

		val response = restTemplate.exchange(
			"/$togglzConsolePath",
			HttpMethod.GET,
			HttpEntity<Void>(headers),
			Void::class.java
		)

		assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
	}

	@Test
	fun whenGet_withOwnerAuthority_returnOk() {
		val headers = HttpHeaders()
		headers.add(HttpHeaders.COOKIE, sessionCookie(generateHttpSession(Authorization.OWNER)))

		val response = restTemplate.exchange(
			"/$togglzConsolePath",
			HttpMethod.GET,
			HttpEntity<Void>(headers),
			Void::class.java
		)

		assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
	}

	private fun sessionCookie(session: Session): String {
		return "SESSION=" + Base64.getEncoder().encodeToString(session.id.toByteArray())
	}

	private fun generateHttpSession(vararg requestedAuthorities: GrantedAuthority): T {
		val userDetails: UserDetails = User(
			"foo",
			"bar",
			true,
			true,
			true,
			true,
			requestedAuthorities.toSet()
		)
		val authentication: Authentication = UsernamePasswordAuthenticationToken(
			userDetails, userDetails.password, userDetails.authorities
		)
		val authenticationToken = UsernamePasswordAuthenticationToken(
			userDetails, authentication.credentials, userDetails.authorities
		)
		authenticationToken.details = authentication.details
		val securityContext: SecurityContext = SecurityContextImpl(authentication)
		val session = sessionRepository.createSession()!!
		session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext)
		session.setAttribute("sessionId", session.id)
		sessionRepository.save(session)
		return session
	}
}
