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
package space.npstr.wolfia.domain.oauth2

import org.immutables.value.Value
import space.npstr.wolfia.db.type.OAuth2Scope
import java.time.Instant

@Value.Immutable
@Value.Style(stagedBuilder = true)
interface AccessTokenResponse {
	fun accessToken(): String?
	fun expires(): Instant?
	fun refreshToken(): String?
	fun scopes(): Set<OAuth2Scope?>?
}
