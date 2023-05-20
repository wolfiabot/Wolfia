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

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import space.npstr.wolfia.App
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor
import space.npstr.wolfia.system.discord.asUserSnowflake
import space.npstr.wolfia.system.logger

@Service
class PrivacyBanService private constructor(
	private val shardManager: ShardManager,
	private val privacyRepository: PrivacyRepository,
	executor: ExceptionLoggingExecutor,
) {

	init {
		executor.scheduleAtFixedRate({ runBlocking { syncBans() } }, 10, 10, TimeUnit.MINUTES)
	}

	@EventListener
	fun onDataDelete(dataDelete: PersonalDataDelete) = runBlocking {
		privacyBanAll(listOf(dataDelete.userId))
	}

	suspend fun syncBans() {
		val allDenied = privacyRepository.findAllDeniedProcessData().map { it.userId }
		privacyBanAll(allDenied)
	}

	suspend fun privacyBanAll(userIds: List<Long>) {
		val homeGuild = shardManager.getGuildById(App.WOLFIA_LOUNGE_ID) ?: return  //we will pick it up on the next sync run
		val bans = try {
			homeGuild.retrieveBanList().submit().await()
		} catch (e: Exception) {
			logger().error("Failed to retrieve ban list", e)
			return
		}

		for (userId in userIds) {
			val isBanned = bans.any { ban -> ban.user.idLong == userId }
			if (isBanned) {
				continue   // nothing to do here
			}
			try {
				homeGuild.ban(userId.asUserSnowflake(), 0, SECONDS)
					.reason("Privacy: Data Processing Denied")
					.submit().await()
			} catch (e: Exception) {
				logger().error("Failed to ban user {}", userId, e)
			}
		}
	}
}
