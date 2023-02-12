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

package space.npstr.wolfia.domain.privacy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.domain.stats.StatsRepository;
import space.npstr.wolfia.system.SessionService;

/**
 * This service finds all the personal data relevant to a user.
 * <p>
 * Note: This class does potentially a heck of a lot blocking requests.
 */
@Service
public class PrivacyRequestService {

    private final StatsRepository statsRepository;
    private final SessionService sessionService;

    private final Cache<Long, PrivacyResponse> privacyResponseCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10000)
            .build();

    public PrivacyRequestService(StatsRepository statsRepository, SessionService sessionService,
                                 CacheMetricsCollector cacheMetricsCollector) {

        this.statsRepository = statsRepository;
        this.sessionService = sessionService;
        cacheMetricsCollector.addCache("privacyResponseCache", this.privacyResponseCache);
    }

    public PrivacyResponse request(long userId) {
        return this.privacyResponseCache.get(userId, this::generate);
    }

    private PrivacyResponse generate(long userId) {
        List<PrivacySession> sessions = this.sessionService.getAllSessionsOfUser(userId).stream()
                .map(this::mapSession)
                .collect(Collectors.toList());

        List<PrivacyGame> games = this.statsRepository.getAllGameStatsOfUser(userId).toCompletableFuture().join();
        Map<Long, List<PrivacyAction>> actions = this.statsRepository.getAllActionStatsOfUser(userId).toCompletableFuture().join();

        games = games.stream()
                .map(game -> {
                    List<PrivacyAction> gameActions = actions.computeIfAbsent(game.getGameId(), __ -> new ArrayList<>());
                    // Use Kotlins copy() call when migrating
                    return new PrivacyGame(
                            game.getGameId(),
                            game.getStartTime(),
                            game.getEndTime(),
                            game.getAlignment(),
                            game.isWinner(),
                            game.getNickname(),
                            game.getTotalPosts(),
                            game.getTotalPostLength(),
                            gameActions
                    );
                })
                .collect(Collectors.toList());
        return new PrivacyResponse(
                sessions,
                games
        );
    }

    private PrivacySession mapSession(Session session) {
        return new PrivacySession(
                session.getCreationTime(),
                session.getLastAccessedTime(),
                session.isExpired()
        );
    }
}
