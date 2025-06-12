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
package space.npstr.wolfia.webapi.guild

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import space.npstr.wolfia.db.type.OAuth2Scope
import space.npstr.wolfia.domain.guild.RemoteGuildService
import space.npstr.wolfia.webapi.WebUser

abstract class GuildEndpoint(
	private val remoteGuildService: RemoteGuildService,
	private val shardManager: ShardManager,
) {

	protected fun assertGuildAccess(user: WebUser?, guildId: Long): WebContext {
		if (user == null) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
		if (!user.hasScope(OAuth2Scope.GUILDS)) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN)
		}
		if (!remoteGuildService.asUser(user).knowsGuild(guildId)) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		val guild = getGuild(guildId)
		val member = getMember(guild, user.id)
		return WebContext(user, guild, member)
	}

	protected fun getGuild(guildId: Long): Guild {
		return shardManager.getGuildById(guildId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
	}

	protected fun getMember(guild: Guild, userId: Long): Member {
		return guild.getMemberById(userId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
	}
}
