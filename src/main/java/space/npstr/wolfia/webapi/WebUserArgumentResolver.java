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

package space.npstr.wolfia.webapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Convenience resolver that allows us to use {@link WebUser} directly in rest controller methods.
 */
@Component
public class WebUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(WebUserArgumentResolver.class);

    private final OAuth2AuthorizedClientManager auth2AuthorizedClientManager;

    public WebUserArgumentResolver(OAuth2AuthorizedClientManager auth2AuthorizedClientManager) {
        this.auth2AuthorizedClientManager = auth2AuthorizedClientManager;
    }

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        MethodParameter methodParameter = parameter.nestedIfOptional();
        Class<?> parameterType = methodParameter.getNestedParameterType();
        return parameterType.equals(WebUser.class);
    }

    @Nullable
    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        WebUser webUser = resolveArgument(webRequest);
        if (parameter.getParameterType() == Optional.class) {
            return Optional.ofNullable(webUser);
        }
        return webUser;
    }

    @Nullable
    private WebUser resolveArgument(NativeWebRequest webRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //noinspection ConditionCoveredByFurtherCondition
        if (authentication == null
                || !(authentication instanceof OAuth2AuthenticationToken)
                || !(authentication.getPrincipal() instanceof OAuth2User)) {
            log.debug("Missing authentication or wrong types");
            return null;
        }
        OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        // We need to rewrite the principal with merged authorities, otherwise it is missing our mapped authories which
        // are only set on the token itself
        Set<GrantedAuthority> mergedAuthorities = new HashSet<>();
        mergedAuthorities.addAll(authenticationToken.getAuthorities());
        mergedAuthorities.addAll(principal.getAuthorities());
        principal = new DefaultOAuth2User(mergedAuthorities, principal.getAttributes(), "id");

        String name = principal.getName();
        long userId;
        try {
            userId = Long.parseLong(name);
        } catch (NumberFormatException e) {
            log.warn("User id '{}' is not a valid snowflake!", name);
            return null;
        }

        String clientRegistrationId = authenticationToken.getAuthorizedClientRegistrationId();
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (request == null) {
            log.debug("Missing request");
            return null;
        }
        if (response == null) {
            log.debug("Missing response");
            return null;
        }
        OAuth2AuthorizeRequest oAuth2AuthorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                .principal(authentication)
                .attribute(HttpServletRequest.class.getName(), request)
                .attribute(HttpServletResponse.class.getName(), response)
                .build();

        OAuth2AuthorizedClient client = this.auth2AuthorizedClientManager.authorize(oAuth2AuthorizeRequest);
        if (client == null) {
            log.debug("Missing OAuth2AuthorizedClient");
            return null;
        }
        OAuth2AccessToken accessToken = client.getAccessToken();
        if (accessToken == null) {
            log.debug("Missing OAuth2AccessToken");
            return null;
        }

        return new WebUser(
                userId,
                principal,
                accessToken
        );
    }
}
