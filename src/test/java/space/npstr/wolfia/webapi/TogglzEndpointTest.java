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

package space.npstr.wolfia.webapi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Togglz Console is served by a Servlet, so we can't use MockMvc.
 */
class TogglzEndpointTest<T extends Session> extends ApplicationTest {

    private static final String TOGGLZ_PATH = "/api/togglz/index";

    private OkHttpClient httpClient;

    @Autowired
    private OkHttpClient.Builder httpClientBuilder;

    @Autowired
    private SessionRepository<T> sessionRepository;

    @BeforeEach
    void setup() {
        httpClient = httpClientBuilder
                .followRedirects(false)
                .build();
    }

    @Test
    void whenGet_withoutAuthentication_returnUnauthorized() throws Exception {
        Request request = getTogglzConsole()
                .build();
        Response response = httpClient.newCall(request).execute();

        assertThat(response.code()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void whenGet_withUserAuthority_returnUnauthorized() throws Exception {
        Session session = generateHttpSession(Authorization.ROLE_USER);
        Request request = getTogglzConsole()
                .header(HttpHeaders.COOKIE, getSessionCookie(session))
                .build();

        Response response = httpClient.newCall(request).execute();

        assertThat(response.code()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void whenGet_withOwnerAuthority_returnOk() throws IOException {
        Session session = generateHttpSession(Authorization.ROLE_OWNER);
        Request request = getTogglzConsole()
                .header(HttpHeaders.COOKIE, getSessionCookie(session))
                .build();

        Response response = httpClient.newCall(request).execute();

        assertThat(response.code()).isEqualTo(HttpStatus.OK.value());
    }

    private Request.Builder getTogglzConsole() {
        return new Request.Builder()
                .get()
                .url("http://localhost:" + port + TOGGLZ_PATH);
    }

    private String getSessionCookie(Session session) {
        return "SESSION=" + Base64.getEncoder().encodeToString(session.getId().getBytes());
    }

    private T generateHttpSession(final String... requestedAuthorities) {
        final Set<GrantedAuthority> authorities =
                Arrays.stream(requestedAuthorities)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());

        final UserDetails userDetails = new User(
                "foo",
                "bar",
                true,
                true,
                true,
                true,
                authorities
        );

        final Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());

        final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, authentication.getCredentials(), userDetails.getAuthorities());
        authenticationToken.setDetails(authentication.getDetails());

        final SecurityContext securityContext = new SecurityContextImpl(authentication);

        T session = sessionRepository.createSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        session.setAttribute("sessionId", session.getId());
        sessionRepository.save(session);
        return session;
    }
}
