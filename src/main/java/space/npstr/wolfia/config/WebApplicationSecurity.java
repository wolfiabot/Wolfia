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

package space.npstr.wolfia.config;

import io.undertow.util.Headers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import space.npstr.wolfia.config.properties.OAuth2Config;
import space.npstr.wolfia.domain.privacy.PrivacyService;
import space.npstr.wolfia.system.ApplicationInfoProvider;
import space.npstr.wolfia.webapi.Authorization;
import space.npstr.wolfia.webapi.LoginRedirect;

@Configuration
public class WebApplicationSecurity {

    private static final Logger log = LoggerFactory.getLogger(WebApplicationSecurity.class);
    private static final String DISCORD_BOT_USER_AGENT = "DiscordBot (https://github.com/wolfiabot/)";

    private static final String[] MACHINE_ENDPOINTS = {
            "/metrics",
    };
    private static final String[] PUBLIC_ENDPOINTS = {
            "/public/**",
            "/index.html",
            "/favicon.ico",
            "/static/**",
            "/",
            "/invite",
    };
    private static final String[] SECURED_ENDPOINTS = {
            "/api/**",
    };

    private final OAuth2Config oAuth2Config;
    private final ApplicationInfoProvider appInfoProvider;
    private final PrivacyService privacyService;

    public WebApplicationSecurity(OAuth2Config oAuth2Config, ShardManager shardManager, PrivacyService privacyService) {
        this.oAuth2Config = oAuth2Config;
        this.appInfoProvider = new ApplicationInfoProvider(shardManager);
        this.privacyService = privacyService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        // https://github.com/spring-projects/spring-security/issues/13568
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);
        MvcRequestMatcher[] noAuthEndpoints = Stream.concat(Arrays.stream(MACHINE_ENDPOINTS), Arrays.stream(PUBLIC_ENDPOINTS))
                .map(mvcMatcherBuilder::pattern)
                .collect(Collectors.toSet())
                .toArray(new MvcRequestMatcher[]{});

        MvcRequestMatcher[] machineEndpoints = Arrays.stream(MACHINE_ENDPOINTS)
                .map(mvcMatcherBuilder::pattern)
                .collect(Collectors.toSet())
                .toArray(new MvcRequestMatcher[]{});

        MvcRequestMatcher[] securedEndpoints = Arrays.stream(SECURED_ENDPOINTS)
                .map(mvcMatcherBuilder::pattern)
                .collect(Collectors.toSet())
                .toArray(new MvcRequestMatcher[]{});


        LoginRedirectHandler loginRedirectHandler = new LoginRedirectHandler(
                this.oAuth2Config.getBaseRedirectUrl(),
                this.privacyService
        );
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // Force CSRF Token creation with Spring Security v6
        // See https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#servlet-defer-loading-csrf-token-opt-out
        requestHandler.setCsrfRequestAttributeName(null);

        return http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(machineEndpoints)
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securedEndpoints).authenticated()
                        .requestMatchers(noAuthEndpoints).permitAll()
                        .anyRequest().permitAll()
                )
                // To avoid redirects to the spring internal login page when unauthorized requests happen to machine endpoints
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(new Http403ForbiddenEntryPoint(), new AntPathRequestMatcher("/api/**"))
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(loginRedirectHandler)
                        .failureHandler(loginRedirectHandler)
                        .tokenEndpoint(it -> it
                                .accessTokenResponseClient(accessTokenResponseClient())
                        )
                        .userInfoEndpoint(it -> it
                                .userService(userService()).userAuthoritiesMapper(authoritiesMapper())
                        )
                )
                .headers(headers -> headers.addHeaderWriter(allowTogglzIFrame()))
                .build();
    }

    private HeaderWriter allowTogglzIFrame() {
        return (request, response) -> {
            if (request.getRequestURI().startsWith("/api/togglz")) {
                response.setHeader(Headers.X_FRAME_OPTIONS_STRING, XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN.name());
            }
        };
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

    private <T> RequestEntity<T> withUserAgent(@Nullable RequestEntity<T> request) {
        if (request == null) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(request.getHeaders());
        headers.add(HttpHeaders.USER_AGENT, DISCORD_BOT_USER_AGENT);

        return new RequestEntity<>(request.getBody(), headers, request.getMethod(), request.getUrl());
    }

    @Bean
    public GrantedAuthoritiesMapper authoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(authorities);

            authorities.forEach(authority -> {
                if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

                    try {
                        String id = (String) userAttributes.get("id");
                        long userId = Long.parseLong(id);
                        if (this.appInfoProvider.isOwner(userId)) {
                            mappedAuthorities.add(Authorization.INSTANCE.getOWNER());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to check for owner id", e);
                    }
                }
            });

            return mappedAuthorities;
        };
    }

    /**
     * Handle a potential login redirect target saved in the session.
     */
    private static final class LoginRedirectHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

        private final String defaultTargetUrl;
        private final PrivacyService privacyService;

        private LoginRedirectHandler(String defaultTargetUrl, PrivacyService privacyService) {
            this.defaultTargetUrl = defaultTargetUrl;
            this.privacyService = privacyService;
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException {

            if (authentication != null && (authentication.getPrincipal() instanceof OAuth2User principal)) {
                String name = principal.getName();
                try {
                    long userId = Long.parseLong(name);
                    if (!this.privacyService.isDataProcessingEnabled(userId)) {
                        SecurityContextHolder.clearContext();
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            session.invalidate();
                        }
                        onLogin(request, response, "no-consent");
                        return;
                    }
                } catch (NumberFormatException e) {
                    log.warn("User id '{}' is not a valid snowflake!", name);
                }
            }

            onLogin(request, response, "success");
        }

        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                            AuthenticationException exception) throws IOException {

            onLogin(request, response, "failed");
        }

        private void onLogin(HttpServletRequest request, HttpServletResponse response, String loginValue) throws IOException {
            String redirectUrl = determineRedirectUrl(request, loginValue);

            response.sendRedirect(redirectUrl);
        }

        private String determineRedirectUrl(HttpServletRequest request, String loginValue) {
            HttpSession session = request.getSession();
            String loginRedirect = (String) session.getAttribute(LoginRedirect.LOGIN_REDIRECT_SESSION_ATTRIBUTE);
            session.removeAttribute(LoginRedirect.LOGIN_REDIRECT_SESSION_ATTRIBUTE);
            if (loginRedirect == null) {
                loginRedirect = this.defaultTargetUrl;
            }
            try {
                HttpUrl httpUrl = HttpUrl.get(loginRedirect).newBuilder()
                        .addQueryParameter("login", loginValue)
                        .build();
                return httpUrl.toString();
            } catch (Exception e) {
                log.warn("Failed to create login redirect URI: {}", loginRedirect);
                return this.defaultTargetUrl;
            }
        }
    }
}
