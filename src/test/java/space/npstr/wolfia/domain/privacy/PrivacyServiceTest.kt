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
package space.npstr.wolfia.domain.privacy

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import space.npstr.wolfia.ApplicationTest
import space.npstr.wolfia.TestUtil
import space.npstr.wolfia.commands.CommandHandler

internal class PrivacyServiceTest<T : Session> : ApplicationTest() {

	@Autowired
	private lateinit var privacyService: PrivacyService

	@Autowired
	@Suppress("SpringJavaInjectionPointsAutowiringInspection")
	private lateinit var sessionRepository: SessionRepository<T>

	@Autowired
	private lateinit var commandHandler: CommandHandler

	/**
	 * See [space.npstr.wolfia.domain.stats.StatsServiceTest] for a more complete test of this
	 */
	@Test
	fun whenDataDelete_gameStatsOfUserAreAnonymized() {
		val userId = TestUtil.uniqueLong()
		privacyService.dataDelete(userId)
		verify(statsService).anonymize(eq(userId))
	}

	@Test
	fun afterDataDelete_commandsByUserAreIgnored() {
		val userId = TestUtil.uniqueLong()
		val user = mock<User>()
		whenever(user.idLong).thenReturn(userId)
		val messageReceived = mock<MessageReceivedEvent>()
		whenever(messageReceived.author).thenReturn(user)
		val message = mock<Message>()
		whenever(message.contentRaw).thenReturn("w.privacy")
		whenever(messageReceived.message).thenReturn(message)
		val channel = mock<MessageChannelUnion>()
		whenever(message.idLong).thenReturn(TestUtil.uniqueLong())
		whenever(messageReceived.channel).thenReturn(channel)
		val jda = mock<JDA>()
		whenever(jda.shardManager).thenReturn(shardManager)
		whenever(messageReceived.jda).thenReturn(jda)
		doAnswer { true }.`when`(privacyCommand).execute(any())
		commandHandler.onMessageReceived(messageReceived)
		verify(privacyCommand, times(1)).execute(any())
		commandHandler.onMessageReceived(messageReceived)
		verify(privacyCommand, times(2)).execute(any())
		privacyService.dataDelete(userId)
		commandHandler.onMessageReceived(messageReceived)
		verify(privacyCommand, times(2)).execute(any())
	}

	@Test
	fun whenDataDelete_sessionsOfUserAreDeleted() {
		val userId = TestUtil.uniqueLong()
		val session = generateHttpSession(userId)
		assertThat(sessionRepository.findById(session.id)).isNotNull
		privacyService.dataDelete(userId)
		assertThat(sessionRepository.findById(session.id)).isNull()
	}

	@Test
	@Disabled
	fun afterDataDelete_userCantLogin() {
		// TODO how do we test this? our login logic is very much bound to Discords OAuth2 api and mocking it is not worth the effort
		throw UnsupportedOperationException()
	}

	/**
	 * See [PrivacyBanServiceTest] for a more complete test of this
	 */
	@Test
	fun whenDataDelete_bannedFromHomeGuild() {
		val userId = TestUtil.uniqueLong()
		privacyService.dataDelete(userId)
		verifyBlocking(privacyBanService) { privacyBanAll(eq(listOf(userId))) }
	}

	private fun generateHttpSession(userId: Long, vararg requestedAuthorities: String): T {
		val authorities = requestedAuthorities
			.map { SimpleGrantedAuthority(it) }
			.toSet()
		val userDetails: UserDetails = org.springframework.security.core.userdetails.User(
			userId.toString(),
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
		val session = sessionRepository.createSession()
		session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext)
		session.setAttribute("sessionId", session.id)
		sessionRepository.save(session)
		return session
	}
}
