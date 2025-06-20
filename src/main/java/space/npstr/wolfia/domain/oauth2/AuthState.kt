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
package space.npstr.wolfia.domain.oauth2

data class AuthState(
	/**
	 * @return id of the user that we sent an authorization link to
	 */
	val userId: Long,
	/**
	 * @return url that can be used to redirect the user after a successful/failed flow, for example to get back
	 * to the channel/message that the flow was started from in discord
	 */
	val redirectUrl: String,
	/**
	 * True if the user should be DMed on success if they added the GUILD_JOIN scope.
	 */
	val guildJoinDm: Boolean,
)
