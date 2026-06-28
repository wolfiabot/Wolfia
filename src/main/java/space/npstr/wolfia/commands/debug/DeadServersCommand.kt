/*
 * Copyright (C) 2016-2026 the original author or authors
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

package space.npstr.wolfia.commands.debug

import java.time.Duration
import java.time.Instant
import space.npstr.wolfia.commands.BaseCommand
import space.npstr.wolfia.commands.CommandContext
import space.npstr.wolfia.domain.Command
import space.npstr.wolfia.domain.room.PrivateRoomService
import space.npstr.wolfia.domain.stats.StatsRepository

@Command
class DeadServersCommand(
	private val statsRepository: StatsRepository,
	private val privateRoomService: PrivateRoomService,
) : BaseCommand {
	override fun getTrigger() = "ds"

	override fun execute(context: CommandContext): Boolean {
		val shardManager = context.jda.shardManager!!
		val privateRooms = privateRoomService.findAll()

		val now = Instant.now()
		val gamesByGuild = statsRepository.countGamesByGuild()

		val deadGuilds = shardManager.guildCache.asSet()
			.filter { guild -> privateRooms.none { it.guildId == guild.idLong } }
			.filter { gamesByGuild[it.idLong] == null || gamesByGuild[it.idLong] == 0 }
			.filter {
				it.selfMember.timeJoined.toInstant()
					.isBefore(now - Duration.ofDays(30))
			}

		val oldest = deadGuilds.sortedBy {
			it.selfMember.timeJoined
		}.take(10)


		val oldestStr = oldest.joinToString("\n") {
			"- ${it.name} (${it.idLong}): ${it.memberCache.size()} Members"
		}
		context.reply("${deadGuilds.count()} guilds have played 0 games.\n$oldestStr")


		if (context.rawArgs.contains("leave")) {
			context.reply("Leaving... ${deadGuilds.size} dead guilds.")
			deadGuilds.forEach { it.leave().complete() }
			context.reply("Done leaving dead guilds.")
		}

		return true
	}

	override fun help() = "Find & purge servers that invited the bot but never played any games in it."
}
