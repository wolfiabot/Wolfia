/*
 * Copyright (C) 2016-2020 the original author or authors
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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.immutables.value.Value
import java.util.Optional

@Value.Immutable
@Value.Style(stagedBuilder = true, strictBuilder = true)
interface SelfUser {
	@get:JsonSerialize(using = ToStringSerializer::class)
	val discordId: Long
	val name: String?
	val discriminator: String?
	val avatarId: Optional<String?>?
	val roles: Set<String?>?
	val scopes: Set<String?>?
}
