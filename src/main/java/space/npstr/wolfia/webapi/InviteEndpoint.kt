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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.sharding.ShardManager
import okhttp3.HttpUrl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.npstr.wolfia.App

/**
 * This endpoint is meant to replace all the various copy&pasted invite links out there.
 *
 *
 * The bonus is that we can generate it on the fly with dynamic parts, like a preselected guild.
 */
@RestController
@RequestMapping("/invite")
class InviteEndpoint(
	private val shardManager: ShardManager,
) {

	/**
	 * Redirect should look something like
	 * https://discord.com/oauth2/authorize?client_id=306583221565521921&scope=bot&permissions=268787777&response_type=code&redirect_uri=https%3A%2F%2Fdiscord.gg%2FnvcfX3q
	 */
	@GetMapping
	fun redirectToInvite(
		@RequestParam(required = false, name = "guild_id") guildId: String?,
		@RequestParam(required = false, name = "redirect_uri") redirectUri: String?,
	): ResponseEntity<Unit> {
		val inviteUrlBuilder = HttpUrl.Builder()
			.scheme("https")
			.host("discord.com")
			.addPathSegment("oauth2")
			.addPathSegment("authorize")
			.addQueryParameter("client_id", botId().toString())
			.addQueryParameter("scope", "bot applications.commands")
			.addQueryParameter(
				"permissions",
				permissions().toString(),
			) // This is a hack that will redirect the user after the invite to the Wolfia Lounge.
			// It is probably a bad idea to redirect to URLs we don't trust as they might receive OAuth2 access tokens of the user.
			.addQueryParameter("response_type", "code")
			.addQueryParameter("redirect_uri", redirectUri ?: App.WOLFIA_LOUNGE_INVITE)
		guildId?.let { inviteUrlBuilder.addQueryParameter("guild_id", it) }
		val headers = HttpHeaders()
		headers.location = inviteUrlBuilder.build().toUri()
		return ResponseEntity(null, headers, HttpStatus.TEMPORARY_REDIRECT)
	}

	private fun permissions(): Long {
		return (
			Permission.MANAGE_ROLES.rawValue
				or Permission.CREATE_INSTANT_INVITE.rawValue
				or Permission.MESSAGE_MANAGE.rawValue
				or Permission.MESSAGE_EMBED_LINKS.rawValue
				or Permission.MESSAGE_HISTORY.rawValue
				or Permission.MESSAGE_ADD_REACTION.rawValue
				or Permission.MESSAGE_EXT_EMOJI.rawValue
			)
	}

	private fun botId(): Long {
		return shardManager.shardCache
			.asSequence()
			.mapNotNull { shard ->
				return@mapNotNull try {
					shard.selfUser
				} catch (e: Exception) {
					null
				}
			}
			.map { it.idLong }
			.first()
	}
}
