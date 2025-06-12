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
package space.npstr.wolfia.webapi.user

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import space.npstr.wolfia.db.type.OAuth2Scope
import space.npstr.wolfia.domain.discord.DiscordRequester
import space.npstr.wolfia.webapi.WebUser

@RestController
@RequestMapping("/public/user")
class UserEndpoint(
	private val discordRequester: DiscordRequester,
) {

	@GetMapping
	fun getSelf(user: WebUser?): ResponseEntity<SelfUser> {
		if (user == null || !user.hasScope(OAuth2Scope.IDENTIFY)) {
			return ResponseEntity(HttpStatus.UNAUTHORIZED)
		}

		// Explicitly fetching the user ensures that the token still works and is not expired or revoked.
		val (userId, name, avatar) = discordRequester.fetchUser(user.accessToken.tokenValue)
		val principal = user.principal
		val roles = filterAndCollectByPrefix(principal.authorities, "ROLE_")
		val scopes = filterAndCollectByPrefix(principal.authorities, "SCOPE_")
		return ResponseEntity.ok(
			SelfUser(userId, name, avatar, roles, scopes),
		)
	}

	private fun filterAndCollectByPrefix(authorities: Collection<GrantedAuthority>, prefix: String): Set<String> {
		return authorities
			.map { it.authority }
			.filter { it.startsWith(prefix) }
			.map { it.substring(prefix.length) }
			.toSet()
	}
}
