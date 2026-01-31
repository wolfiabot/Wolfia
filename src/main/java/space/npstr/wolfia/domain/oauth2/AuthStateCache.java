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

package space.npstr.wolfia.domain.oauth2;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.prometheus.metrics.instrumentation.caffeine.CacheMetricsCollector;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * This component serves two purposes: It secures the OAuth2 flow (see https://discord.com/developers/docs/topics/oauth2#state-and-security)
 * and it allows us to redirect the user back to whereever they started the flow from.
 */
@Component
public class AuthStateCache {

    // state <-> oauth2 state
    private final Cache<String, AuthState> stateCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(10000)
            .build();

    public AuthStateCache(CacheMetricsCollector cacheMetricsCollector) {
        cacheMetricsCollector.addCache("oAuth2StateCache", this.stateCache);
    }

    public String generateStateParam(AuthState authState) {
        String state = UUID.randomUUID().toString().replace("-", "");
        this.stateCache.put(state, authState);
        return state;
    }

    public Optional<AuthState> getAuthState(@Nullable String state) {
        return Optional.ofNullable(state)
                .flatMap(s -> Optional.ofNullable(this.stateCache.getIfPresent(s)));
    }
}
