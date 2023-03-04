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
package space.npstr.wolfia.domain.oauth2

import java.io.IOException
import java.time.Instant
import java.util.concurrent.CompletableFuture
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials.basic
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import space.npstr.prometheus_extensions.OkHttpEventCounter
import space.npstr.wolfia.config.properties.OAuth2Config
import space.npstr.wolfia.db.type.OAuth2Scope
import space.npstr.wolfia.db.type.OAuth2Scope.GUILD_JOIN
import space.npstr.wolfia.db.type.OAuth2Scope.IDENTIFY
import space.npstr.wolfia.system.logger
import space.npstr.wolfia.webapi.OAuth2Endpoint

/**
 * Handle http request stuff related to oauth2
 */
@Component
class OAuth2Requester(
	private val oAuth2Config: OAuth2Config,
	httpClientBuilder: OkHttpClient.Builder,
) {

	private val httpClient: OkHttpClient

	init {
		httpClient = httpClientBuilder
			.eventListener(OkHttpEventCounter("oauth2"))
			.build()
	}

	companion object {
		const val SCOPE_DELIMITER = " "

		//see https://discord.com/developers/docs/topics/oauth2#shared-resources-oauth2-urls
		private const val AUTHORIZATION_URL = "https://discord.com/api/oauth2/authorize"
		private const val TOKEN_URL = "https://discord.com/api/oauth2/token"
		private const val REVOCATION_URL = "https://discord.com/api/oauth2/token/revoke"
		private const val DISCORD_API = "https://discord.com/api/v6"
		private const val GET_USER_URL = "$DISCORD_API/users/@me"
	}


	/**
	 * @return the url that initializes oauth2 authorization with discord
	 */
	fun authorizationUrl(state: String): HttpUrl {
		return AUTHORIZATION_URL.toHttpUrl().newBuilder()
			.addQueryParameter("client_id", oAuth2Config.clientId)
			.addQueryParameter("redirect_uri", redirectUri())
			.addQueryParameter("state", state)
			.addQueryParameter("response_type", "code")
			.addQueryParameter("scope", scopesAsString(IDENTIFY, GUILD_JOIN))
			.build()
	}

	/**
	 * See https://discord.com/developers/docs/topics/oauth2#authorization-code-grant-access-token-exchange-example
	 */
	suspend fun fetchCodeResponse(code: String): AccessTokenResponse {
		val requestBody: RequestBody = FormBody.Builder()
			.add("grant_type", "authorization_code")
			.add("code", code)
			.add("redirect_uri", redirectUri())
			.add("scope", scopesAsString(IDENTIFY, GUILD_JOIN))
			.build()
		val codeRequest: Request = Request.Builder()
			.url(TOKEN_URL)
			.header(HttpHeaders.AUTHORIZATION, basic(oAuth2Config.clientId, oAuth2Config.clientSecret))
			.post(requestBody)
			.build()
		val oAuth2Callback = CompletableFuture<Response>()
		httpClient.newCall(codeRequest)
			.enqueue(asCallback(oAuth2Callback))
		val response = oAuth2Callback.await()
		return toAccessTokenResponse(response)
	}

	/**
	 * See https://discord.com/developers/docs/resources/user#get-current-user
	 *
	 * @return the id of the user who the passed in accessToken belongs to
	 */
	suspend fun identifyUser(accessToken: String): Long {
		val userInfoRequest: Request = Request.Builder()
			.url(GET_USER_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
			.get()
			.build()
		val userInfoCallback = CompletableFuture<Response>()
		httpClient.newCall(userInfoRequest)
			.enqueue(asCallback(userInfoCallback))
		val response = userInfoCallback.await()
		return extractUserId(response)
	}

	fun refreshBlocking(old: OAuth2Data): OAuth2Data {
		return runBlocking { refresh(old) }
	}

	/**
	 * See https://discord.com/developers/docs/topics/oauth2#authorization-code-grant-refresh-token-exchange-example
	 *
	 * @return a refreshed [OAuth2Data]
	 */
	suspend fun refresh(old: OAuth2Data): OAuth2Data {
		val requestBody: RequestBody = FormBody.Builder()
			.add("grant_type", "refresh_token")
			.add("refresh_token", old.refreshToken())
			.add("redirect_uri", redirectUri())
			.add("scope", scopesAsString(old.scopes()))
			.build()
		val refreshRequest = Request.Builder()
			.url(TOKEN_URL)
			.header(HttpHeaders.AUTHORIZATION, basic(oAuth2Config.clientId, oAuth2Config.clientSecret))
			.post(requestBody)
			.build()
		val refreshCallback = CompletableFuture<Response>()
		httpClient.newCall(refreshRequest)
			.enqueue(asCallback(refreshCallback))

		val response = refreshCallback.await()
		val (accessToken, expires, refreshToken, scopes) = toAccessTokenResponse(response)
		return OAuth2Data(old.userId(), accessToken, expires, refreshToken, scopes)
	}

	private fun toAccessTokenResponse(response: Response): AccessTokenResponse {
		val body = response.use { unwrapBody(response) }
		val json = JSONObject(body)
		val accessToken = json.getString("access_token")
		val refreshToken = json.getString("refresh_token")
		val expiresInSeconds = json.getInt("expires_in")
		val expires = Instant.now().plusSeconds(expiresInSeconds.toLong())
		val scope = json.getString("scope")

		val scopes = scope.split(SCOPE_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			.mapNotNull {
				val parsed = OAuth2Scope.parse(it)
				if (parsed.isEmpty) {
					logger().warn("Unknown scope: {}", it)
				}
				parsed.getOrNull()
			}
			.toSet()

		return AccessTokenResponse(accessToken, expires, refreshToken, scopes)
	}

	private fun extractUserId(response: Response): Long {
		val body = response.use { unwrapBody(response) }
		val json = JSONObject(body)
		val id = json.getString("id")
		return try {
			id.toLong()
		} catch (e: NumberFormatException) {
			throw DiscordRequestFailedException("Discord returned a userId that is not a long, body: $body", e)
		}
	}

	private fun redirectUri(): String {
		var result = oAuth2Config.baseRedirectUrl
		if (!result.endsWith("/")) {
			result += "/"
		}
		result += OAuth2Endpoint.CODE_GRANT_PATH
		return result
	}

	private fun scopesAsString(vararg scopes: OAuth2Scope): String {
		return scopesAsString(setOf(*scopes))
	}

	private fun scopesAsString(scopes: Set<OAuth2Scope>): String {
		return scopes.joinToString(SCOPE_DELIMITER) { it.discordName() }
	}

	private fun unwrapBody(response: Response): String {
		if (!response.isSuccessful || response.body == null) {
			response.close()
			throw DiscordRequestFailedException("Failed to call ddoscord, status " + response.code)
		}
		val body: String = try {
			response.body!!.string()
		} catch (e: IOException) {
			response.close()
			throw DiscordRequestFailedException("Failed to get body from ddoscord response, status " + response.code, e)
		}
		return body
	}

	private fun asCallback(completableFuture: CompletableFuture<Response>): Callback {
		return object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				completableFuture.completeExceptionally(e)
			}

			override fun onResponse(call: Call, response: Response) {
				completableFuture.complete(response)
			}
		}
	}
}
