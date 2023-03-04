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
package space.npstr.wolfia.domain.guild

import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import space.npstr.wolfia.domain.discord.DiscordRequester
import space.npstr.wolfia.domain.discord.PartialGuild
import space.npstr.wolfia.webapi.WebUser

/**
 * Fetch guilds on behalf of a user from Discord
 */
@Component
class RemoteGuildService(
	private val discordRequester: DiscordRequester,
	private val shardManager: ShardManager,
) {

	companion object {
		private val EDIT_PERMISSION = Permission.ADMINISTRATOR
		private val CACHE_DURATION = Duration.ofSeconds(30)
	}

	private val cache = Caffeine.newBuilder()
		.expireAfterWrite(CACHE_DURATION)
		.build<WebUser, List<PartialGuild>>()

	fun asUser(webUser: WebUser): Action {
		return Action(webUser)
	}

	inner class Action internal constructor(private val webUser: WebUser) {

		fun knowsGuild(guildId: Long): Boolean {
			return fetchGuild(guildId) != null
		}

		fun fetchGuild(guildId: Long): GuildInfo? {
			return fetchAllGuilds().firstOrNull { it.guild.id == guildId }
		}

		fun fetchAllGuilds(): List<GuildInfo> {
			val partialGuilds = cache.get(webUser) {
				discordRequester.fetchAllGuilds(it.accessToken.tokenValue)
			} ?: listOf()

			return partialGuilds.map { toGuildInfo(it, webUser.id) }
		}

		private fun toGuildInfo(partialGuild: PartialGuild, userId: Long): GuildInfo {
			val guild = shardManager.guildCache.getElementById(partialGuild.id)
			var canEdit = false
			if (guild != null) {
				val member = guild.getMemberById(userId)
				if (member != null) {
					canEdit = member.hasPermission(EDIT_PERMISSION)
				}
			}
			return GuildInfo(partialGuild, guild != null, canEdit)
		}
	}
}
