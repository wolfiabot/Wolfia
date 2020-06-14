/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
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

    private final OAuth2AuthorizedClientRepository oAuth2Repository;

    public WebUserArgumentResolver(OAuth2AuthorizedClientRepository oAuth2Repository) {
        this.oAuth2Repository = oAuth2Repository;
    }

    @Override
    public boolean supportsParameter(@Nonnull MethodParameter parameter) {
        MethodParameter methodParameter = parameter.nestedIfOptional();
        Class<?> parameterType = methodParameter.getNestedParameterType();
        return parameterType.equals(WebUser.class);
    }

    @Nullable
    @Override
    public Object resolveArgument(
            @Nonnull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @Nonnull NativeWebRequest webRequest,
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
        String userIdStr = (String) principal.getAttributes().get("id");
        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.warn("User id '{}' is not a valid long!", userIdStr);
            return null;
        }

        String clientRegistrationId = authenticationToken.getAuthorizedClientRegistrationId();
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            log.debug("Missing request");
            return null;
        }
        OAuth2AuthorizedClient client = this.oAuth2Repository.loadAuthorizedClient(clientRegistrationId, authentication, request);
        if (client == null) {
            log.debug("Missing OAuth2AuthorizedClient");
            return null;
        }
        OAuth2AccessToken accessToken = client.getAccessToken();
        if (accessToken == null) {
            log.debug("Missing OAuth2AccessToken");
            return null;
        }

        return ImmutableWebUser.builder()
                .id(userId)
                .principal(principal)
                .accessToken(accessToken)
                .build();
    }
}
