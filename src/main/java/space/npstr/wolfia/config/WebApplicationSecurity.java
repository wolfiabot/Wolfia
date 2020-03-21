/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

package space.npstr.wolfia.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import space.npstr.wolfia.App;
import space.npstr.wolfia.webapi.Authorization;
import space.npstr.wolfia.webapi.LoginRedirect;

@Configuration
public class WebApplicationSecurity extends WebSecurityConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebApplicationSecurity.class);
    private static final String DISCORD_BOT_USER_AGENT = "DiscordBot (https://github.com/wolfiabot/)";

    private static final String[] MACHINE_ENDPOINTS = {
            "/metrics",
    };
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/oauth2/**",
            "/api/test/**",
            LoginRedirect.ROUTE,
            "/index.html",
            "/static/**",
            "/"
    };

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String[] noAuthEndpoints = Stream.concat(Arrays.stream(MACHINE_ENDPOINTS), Arrays.stream(PUBLIC_ENDPOINTS))
                .collect(Collectors.toSet())
                .toArray(new String[]{});

        http
                .csrf().ignoringAntMatchers(MACHINE_ENDPOINTS)
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and()
                .authorizeRequests()
                .antMatchers(noAuthEndpoints).permitAll()
                .anyRequest().authenticated()
                .and().oauth2Login().tokenEndpoint().accessTokenResponseClient(accessTokenResponseClient())
                .and().userInfoEndpoint().userService(userService()).userAuthoritiesMapper(authoritiesMapper())
        ;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();

        client.setRequestEntityConverter(new OAuth2AuthorizationCodeGrantRequestEntityConverter() {
            @Override
            public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest oauth2Request) {
                return withUserAgent(super.convert(oauth2Request));
            }
        });

        return client;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> userService() {
        DefaultOAuth2UserService service = new DefaultOAuth2UserService();

        service.setRequestEntityConverter(new OAuth2UserRequestEntityConverter() {
            @Override
            public RequestEntity<?> convert(OAuth2UserRequest userRequest) {
                return withUserAgent(super.convert(userRequest));
            }
        });

        return service;
    }


    @Bean
    public GrantedAuthoritiesMapper authoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OAuth2UserAuthority) {
                    OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;
                    Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();


                    try {
                        String id = (String) userAttributes.get("id");
                        long userId = Long.parseLong(id);
                        if (userId == App.OWNER_ID) {
                            mappedAuthorities.add(Authorization.OWNER);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to check for owner id", e);
                    }
                    mappedAuthorities.add(Authorization.USER);
                }
            });

            return mappedAuthorities;
        };
    }

    private <T> RequestEntity<T> withUserAgent(@Nullable RequestEntity<T> request) {
        if (request == null) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.getHeaders());
        headers.add(HttpHeaders.USER_AGENT, DISCORD_BOT_USER_AGENT);

        return new RequestEntity<>(request.getBody(), headers, request.getMethod(), request.getUrl());
    }
}
