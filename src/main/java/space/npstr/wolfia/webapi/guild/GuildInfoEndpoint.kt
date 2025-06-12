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

import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import space.npstr.wolfia.db.type.OAuth2Scope
import space.npstr.wolfia.domain.guild.GuildInfo
import space.npstr.wolfia.domain.guild.RemoteGuildService
import space.npstr.wolfia.webapi.WebUser

@RestController
@RequestMapping("/api")
class GuildInfoEndpoint(
	private val remoteGuildService: RemoteGuildService,
	shardManager: ShardManager,
) : GuildEndpoint(remoteGuildService, shardManager) {

	@GetMapping("/guild/{guildId}")
	fun getGuild(@PathVariable guildId: Long, user: WebUser?): ResponseEntity<GuildInfo> {
		val context = assertGuildAccess(user, guildId)

		return this.remoteGuildService.asUser(context.user)
			.fetchGuild(guildId)
			?.let { ResponseEntity.ok(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
	}

	@GetMapping("/guilds")
	fun getGuilds(user: WebUser?): ResponseEntity<List<GuildInfo>> {
		if (user == null) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
		if (!user.hasScope(OAuth2Scope.GUILDS)) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN)
		}
		return ResponseEntity.ok(
			this.remoteGuildService.asUser(user)
				.fetchAllGuilds(),
		)
	}
}
