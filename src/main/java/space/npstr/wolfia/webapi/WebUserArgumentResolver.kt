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
package space.npstr.wolfia.webapi

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.Optional
import org.springframework.core.MethodParameter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import space.npstr.wolfia.system.logger

/**
 * Convenience resolver that allows us to use [WebUser] directly in rest controller methods.
 */
@Component
class WebUserArgumentResolver(
	private val auth2AuthorizedClientManager: OAuth2AuthorizedClientManager,
) : HandlerMethodArgumentResolver {

	override fun supportsParameter(parameter: MethodParameter): Boolean {
		val methodParameter = parameter.nestedIfOptional()
		val parameterType = methodParameter.nestedParameterType
		return parameterType == WebUser::class.java
	}

	override fun resolveArgument(
		parameter: MethodParameter,
		mavContainer: ModelAndViewContainer?,
		webRequest: NativeWebRequest,
		binderFactory: WebDataBinderFactory?,
	): Any? {

		val webUser = resolveArgument(webRequest)
		return if (parameter.parameterType == Optional::class.java) {
			Optional.ofNullable(webUser)
		} else {
			webUser
		}
	}

	private fun resolveArgument(webRequest: NativeWebRequest): WebUser? {
		val authentication = SecurityContextHolder.getContext().authentication
		if (authentication == null || authentication !is OAuth2AuthenticationToken
			|| authentication.principal !is OAuth2User
		) {
			logger().debug("Missing authentication or wrong types")
			return null
		}
		var principal = authentication.principal as OAuth2User

		// We need to rewrite the principal with merged authorities, otherwise it is missing our mapped authories which
		// are only set on the token itself
		val mergedAuthorities: MutableSet<GrantedAuthority> = HashSet()
		mergedAuthorities.addAll(authentication.authorities)
		mergedAuthorities.addAll(principal.authorities)
		principal = DefaultOAuth2User(mergedAuthorities, principal.attributes, "id")
		val name = principal.name
		val userId: Long = try {
			name.toLong()
		} catch (_: NumberFormatException) {
			logger().warn("User id '{}' is not a valid snowflake!", name)
			return null
		}
		val clientRegistrationId = authentication.authorizedClientRegistrationId
		val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
		val response = webRequest.getNativeResponse(HttpServletResponse::class.java)
		if (request == null) {
			logger().debug("Missing request")
			return null
		}
		if (response == null) {
			logger().debug("Missing response")
			return null
		}
		val oAuth2AuthorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
			.principal(authentication)
			.attribute(HttpServletRequest::class.java.name, request)
			.attribute(HttpServletResponse::class.java.name, response)
			.build()
		val client = auth2AuthorizedClientManager.authorize(oAuth2AuthorizeRequest)
		if (client == null) {
			logger().debug("Missing OAuth2AuthorizedClient")
			return null
		}
		val accessToken = client.accessToken
		if (accessToken == null) {
			logger().debug("Missing OAuth2AccessToken")
			return null
		}

		return WebUser(userId, principal, accessToken)
	}
}
