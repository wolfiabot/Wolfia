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

package space.npstr.wolfia.domain.discord;

import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpSession;
import okhttp3.OkHttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import space.npstr.prometheus_extensions.OkHttpEventCounter;

/**
 * Run our own requests against the Discord Api on behalf of users using their access tokens (OAuth2).
 * <p>
 * TODO we need a proper ratelimiter implementation for discord requests
 */
@Component
public class DiscordRequester {

    private static final String DISCORD_API_URL = "https://discord.com/api/v6";

    private final RestTemplate restTemplate;

    public DiscordRequester(OkHttpClient.Builder httpClientBuilder) {
        OkHttpClient httpClient = httpClientBuilder
                .eventListener(new OkHttpEventCounter("oauth2"))
                .build();
        this.restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> new OkHttp3ClientHttpRequestFactory(httpClient))
                .rootUri(DISCORD_API_URL)
                .build();
    }

    public PartialUser fetchUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        ResponseEntity<PartialUser> exchange;
        try {
            exchange = this.restTemplate.exchange(
                    "/users/@me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            throw handleUnauthorized();
        }

        PartialUser user = exchange.getBody();
        Objects.requireNonNull(user, "fetched user is null");
        return user;
    }

    public List<PartialGuild> fetchAllGuilds(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        ResponseEntity<List<PartialGuild>> exchange;
        try {
            exchange = this.restTemplate.exchange(
                    "/users/@me/guilds",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            throw handleUnauthorized();
        }

        List<PartialGuild> guilds = exchange.getBody();
        Objects.requireNonNull(guilds, "fetched guilds are null");
        return guilds;
    }

    private ResponseStatusException handleUnauthorized() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        SecurityContextHolder.clearContext();
        HttpSession session = requestAttributes.getRequest().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
}
