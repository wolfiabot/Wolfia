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
package space.npstr.wolfia.domain.guild

import space.npstr.wolfia.domain.discord.PartialGuild

data class GuildInfo(
	/**
	 * The information we get from Discord about a guild.
	 */
	val guild: PartialGuild,
	/**
	 * Is our bot present in this guild?
	 */
	val botPresent: Boolean,
	/**
	 * Can the user edit this guild? This is just a convenience property for the frontend UI.
	 * There are additional checks in the backend for concrete requests.
	 */
	val canEdit: Boolean,
)
