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

import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import space.npstr.wolfia.App
import space.npstr.wolfia.ApplicationTest
import space.npstr.wolfia.TestUtil
import space.npstr.wolfia.utils.discord.EmptyRestAction

internal class PrivacyBanServiceTest : ApplicationTest() {

	@Test
	fun whenBanAll_getsBanned() {
		val userId = TestUtil.uniqueLong()
		val userIdStr = userId.toString()
		val restAction = mock<AuditableRestAction<Void>>()
		val wolfiaLounge = mock<Guild> {
			on { ban(eq(userIdStr), eq(0), eq("Privacy: Data Processing Denied")) } doReturn restAction
			on { retrieveBanList() } doReturn EmptyRestAction(null, listOf())
		}
		whenever(shardManager.getGuildById(eq(App.WOLFIA_LOUNGE_ID))).thenReturn(wolfiaLounge)

		runBlocking { privacyBanService.privacyBanAll(listOf(userId)) }
		verify(wolfiaLounge).ban(eq(userIdStr), eq(0), eq("Privacy: Data Processing Denied"))
		Unit
	}
}
