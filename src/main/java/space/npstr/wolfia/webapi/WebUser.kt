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

import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.user.OAuth2User
import space.npstr.wolfia.db.type.OAuth2Scope

/**
 * User that is doing the web request
 */
data class WebUser(
	/**
	 * discord id of the user
	 */
	val id: Long,
	/**
	 * Principal user object with further
	 */
	val principal: OAuth2User,
	/**
	 * Oauth2 access token
	 */
	val accessToken: OAuth2AccessToken,
) {

	/**
	 * @return true if this web user has access to the requested scope
	 */
	fun hasScope(scope: OAuth2Scope): Boolean {
		return accessToken.scopes.contains(scope.discordName())
	}

}
