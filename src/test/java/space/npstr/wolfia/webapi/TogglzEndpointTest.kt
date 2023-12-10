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

import java.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
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

    companion object {
        private const val TOGGLZ_PATH = "/api/togglz/index"
    }

    private lateinit var httpClient: OkHttpClient

    @Autowired
    private lateinit var httpClientBuilder: OkHttpClient.Builder

    @Autowired
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private lateinit var sessionRepository: SessionRepository<T>

    @BeforeEach
    fun setup() {
        httpClient = httpClientBuilder
            .followRedirects(false)
            .build()
    }

    @Test
    fun whenGet_withoutAuthentication_returnUnauthorized() {
        val request = togglzConsole().build()

        val response = httpClient.newCall(request).execute()

        assertThat(response.code).isEqualTo(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun whenGet_withUserAuthority_returnUnauthorized() {
        val session = generateHttpSession(Authorization.ROLE_USER)
        val request = togglzConsole()
            .header(HttpHeaders.COOKIE, sessionCookie(session))
            .build()

        val response = httpClient.newCall(request).execute()

        assertThat(response.code).isEqualTo(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun whenGet_withOwnerAuthority_returnOk() {
        val session = generateHttpSession(Authorization.ROLE_OWNER)
        val request = togglzConsole()
            .header(HttpHeaders.COOKIE, sessionCookie(session))
            .build()

        val response = httpClient.newCall(request).execute()

        assertThat(response.code).isEqualTo(HttpStatus.OK.value())
    }

    private fun togglzConsole(): Request.Builder {
        return Request.Builder()
            .get()
            .url("http://localhost:$port$TOGGLZ_PATH")
    }

    private fun sessionCookie(session: Session): String {
        return "SESSION=" + Base64.getEncoder().encodeToString(session.id.toByteArray())
    }

    private fun generateHttpSession(vararg requestedAuthorities: String): T {
        val authorities = requestedAuthorities
            .map { SimpleGrantedAuthority(it) }
            .toSet()

        val userDetails: UserDetails = User(
            "foo",
            "bar",
            true,
            true,
            true,
            true,
            authorities
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
