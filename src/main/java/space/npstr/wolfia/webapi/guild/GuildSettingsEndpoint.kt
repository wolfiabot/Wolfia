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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import space.npstr.wolfia.domain.guild.RemoteGuildService
import space.npstr.wolfia.domain.settings.ChannelSettingsService
import space.npstr.wolfia.domain.settings.GuildSettings
import space.npstr.wolfia.domain.settings.GuildSettingsService
import space.npstr.wolfia.webapi.WebUser

@RestController
@RequestMapping("/api/guild_settings")
class GuildSettingsEndpoint(
	remoteGuildService: RemoteGuildService,
	shardManager: ShardManager,
	private val guildSettingsService: GuildSettingsService,
	private val channelSettingsService: ChannelSettingsService,
) : GuildEndpoint(remoteGuildService, shardManager) {

	@GetMapping("/{guildId}")
	operator fun get(@PathVariable guildId: Long, user: WebUser?): ResponseEntity<GuildSettingsVO> {
		val context = assertGuildAccess(user, guildId)
		val guildSettings = guildSettingsService.guild(guildId).getOrDefault()
		return ResponseEntity.ok(toValueObject(guildSettings, collectChannelSettings(context.member)))
	}

	@PostMapping("/{guildId}/channel_settings/game_channel")
	fun setGameChannels(
		@PathVariable guildId: Long,
		@RequestBody body: List<GameChannelRequest>,
		user: WebUser?,
	): ResponseEntity<GuildSettingsVO> {
		val context = assertGuildAccess(user, guildId)
		for ((channelId, isGameChannel) in body) {
			setGameChannel(context, channelId, isGameChannel)
		}
		val guildSettings = guildSettingsService.guild(context.guild.idLong).getOrDefault()
		return ResponseEntity.ok(toValueObject(guildSettings, collectChannelSettings(context.member)))
	}

	@PostMapping("/{guildId}/channel_settings/{channelId}/game_channel/{isGameChannel}")
	fun setGameChannel(
		@PathVariable guildId: Long,
		@PathVariable channelId: Long,
		@PathVariable isGameChannel: Boolean,
		user: WebUser?,
	): ResponseEntity<GuildSettingsVO> {
		val context = assertGuildAccess(user, guildId)
		setGameChannel(context, channelId, isGameChannel)
		val guildSettings = guildSettingsService.guild(context.guild.idLong).getOrDefault()
		return ResponseEntity.ok(toValueObject(guildSettings, collectChannelSettings(context.member)))
	}

	private fun setGameChannel(context: WebContext, channelId: Long, isGameChannel: Boolean) {
		val textChannel = context.guild.getTextChannelById(channelId)
		if (textChannel == null) { //if the channel has been deleted, we might as well remove it without further checks
			channelSettingsService.channel(channelId).reset()
			return
		}
		if (!context.member.hasPermission(textChannel, Permission.VIEW_CHANNEL)) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN)
		}
		val action = channelSettingsService.channel(channelId)
		if (isGameChannel) {
			action.enableGameChannel()
		} else {
			action.disableGameChannel()
		}
	}

	private fun collectChannelSettings(member: Member): List<ChannelSettingsVO> {
		val textChannels = member.guild.textChannels
			.filter { member.hasPermission(it, Permission.VIEW_CHANNEL) }
		val textChannelIds = textChannels.map { it.idLong }

		val channelSettings = channelSettingsService.channels(textChannelIds)
			.getOrDefault()
			.associateBy { it.channelId }
			.toMutableMap()

		return textChannels
			.map { textChannel ->
				val isGameChannel = channelSettings.computeIfAbsent(textChannel.idLong) { id ->
					channelSettingsService.channel(id).getOrDefault()
				}.isGameChannel

				return@map ChannelSettingsVO(
					textChannel.idLong,
					textChannel.name,
					isGameChannel,
				)
			}
	}

	private fun toValueObject(guildSettings: GuildSettings, channelSettingsList: List<ChannelSettingsVO>): GuildSettingsVO {
		return GuildSettingsVO(
			guildSettings.guildId,
			channelSettingsList.toSet(),
		)
	}
}
