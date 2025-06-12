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
package space.npstr.wolfia.domain.settings

import net.dv8tion.jda.api.entities.Guild
import org.springframework.stereotype.Service

@Service
class GuildSettingsService private constructor(private val repository: GuildSettingsRepository) {

	fun set(guild: Guild): GuildSettings {
		return repository.set(guild.idLong, guild.name, guild.iconId)
	}

	/**
	 * This service has many calls that require passing in multiple long ids. This fluent action api should help avoid
	 * mistakes where arguments are passed in the wrong order.
	 *
	 * @return an action that can be executed on the passed in guild
	 */
	fun guild(guildId: Long): Action {
		return Action(guildId)
	}

	inner class Action internal constructor(private val guildId: Long) {

		fun getOrDefault(): GuildSettings {
			return repository.findOneOrDefault(guildId)
		}
	}
}
