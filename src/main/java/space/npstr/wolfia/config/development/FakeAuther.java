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

package space.npstr.wolfia.config.development;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import space.npstr.wolfia.SpringProfiles;
import space.npstr.wolfia.webapi.Authorization;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

/**
 * This will authenticate any request by any user with a fake user based on real login by Napster.
 * Helpful for development without having to go through the whole OAuth2 flow all the time which requires ngrok or similar,
 * and stops us from using the Vue/Yarn dev server for frontend stuff.
 */
@Profile(SpringProfiles.FAKE_LOGIN)
@Component
public class FakeAuther implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FakeAuther.class);

    private final GrantedAuthoritiesMapper grantedAuthoritiesMapper;

    public FakeAuther(GrantedAuthoritiesMapper grantedAuthoritiesMapper) {
        this.grantedAuthoritiesMapper = grantedAuthoritiesMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication existingAuth = securityContext.getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated() && existingAuth.getAuthorities().contains(Authorization.USER)) {
            return true; //nothing to do here
        }

        // Dummy values based on a real login of my own account
        Map<String, Object> attributes = Map.of(
                "id", "166604053629894657",
                "username", "User McUserface",
                "avatar", "",
                "discriminator", randomDiscrim(),
                "locale", "en-US",
                "mfa_enabled", true,
                "flags", 640,
                "premium_type", 1
        );
        var authorities = List.of(
                new OAuth2UserAuthority(attributes),
                new SimpleGrantedAuthority("SCOPE_identity")
        );

        OAuth2User principal = new DefaultOAuth2User(authorities, attributes, "username");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                grantedAuthoritiesMapper.mapAuthorities(authorities),
                "discord");
        authentication.setAuthenticated(true);
        securityContext.setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);

        log.info("Fake authenticated user");
        return true;
    }

    private String randomDiscrim() {
        int randomDiscrim = ThreadLocalRandom.current().nextInt(1, 10000);
        StringBuilder result = new StringBuilder(Integer.toString(randomDiscrim));
        while (result.length() < 4) {
            result.insert(0, "0");
        }
        return result.toString();
    }
}
