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

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import space.npstr.wolfia.db.type.OAuth2Scope

@Service
class OAuth2Service internal constructor(
	private val repository: OAuth2Repository,
	private val oAuth2Requester: OAuth2Requester,
) {

	/**
	 * @return the oauth2 access token for the requested user and scope, if such a token exists in our database,
	 * empty otherwise
	 * Note: the returned token may be invalid if the user revoked it
	 */
	fun getAccessTokenForScope(userId: Long, scope: OAuth2Scope): String? {
		val data = repository.findOne(userId) ?: return null
		if (Instant.now().isAfter(data.expires())) {
			return null
		}
		if (!data.scopes().contains(scope)) {
			return null
		}
		return data.accessToken()
	}

	/**
	 * This completes the OAuth2 flow by fetching and saving the [OAuth2Data] of a user who authorized us.
	 *
	 * @param code code that we receive from Discord once a user visits our OAuth2 authorization url and authorizes us.
	 */
	suspend fun acceptCode(code: String): OAuth2Data {
		val (accessToken, expires, refreshToken, scopes) = oAuth2Requester.fetchCodeResponse(code)
		val userId = oAuth2Requester.identifyUser(accessToken)
		return repository.save(OAuth2Data(userId, accessToken, expires, refreshToken, scopes, Instant.now()))
	}

	fun acceptCodeBlocking(code: String): OAuth2Data {
		return runBlocking { acceptCode(code) }
	}
}
