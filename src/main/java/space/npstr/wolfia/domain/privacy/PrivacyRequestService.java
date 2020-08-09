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

package space.npstr.wolfia.domain.privacy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.domain.stats.ActionStats;
import space.npstr.wolfia.domain.stats.GameStats;
import space.npstr.wolfia.domain.stats.PlayerStats;
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

        List<PrivacyGame> games = this.statsRepository.findGameIdsByUser(userId)
                .toCompletableFuture().join()
                .stream()
                .map(gameId -> this.statsRepository.findGameStats(gameId).toCompletableFuture().join())
                .map(gameOpt -> gameOpt.orElse(null))
                .filter(Objects::nonNull)
                .map(gameStats -> this.mapGame(gameStats, userId))
                .collect(Collectors.toList());

        return ImmutablePrivacyResponse.builder()
                .addAllSessions(sessions)
                .addAllGames(games)
                .build();
    }

    private PrivacySession mapSession(Session session) {
        return ImmutablePrivacySession.builder()
                .creationTime(session.getCreationTime())
                .lastAccessedTime(session.getLastAccessedTime())
                .isExpired(session.isExpired())
                .build();
    }

    private PrivacyGame mapGame(GameStats gameStats, long userId) {
        PlayerStats player = gameStats.getStartingTeams().stream()
                .flatMap(team -> team.getPlayers().stream())
                .filter(p -> p.getUserId() == userId)
                .findAny()
                .orElseThrow();

        return ImmutablePrivacyGame.builder()
                .gameId(gameStats.getGameId().orElseThrow())
                .alignment(player.getAlignment().name())
                .isWinner(player.getTeam().isWinner())
                .startDate(Instant.ofEpochMilli(gameStats.getStartTime()))
                .endDate(Instant.ofEpochMilli(gameStats.getEndTime()))
                .nickname(player.getNickname())
                .totalPosts(player.getTotalPosts())
                .totalPostLength(player.getTotalPostLength())
                .addAllActions(gameStats.getActions().stream()
                        .filter(action -> action.getActor() == player.getPlayerId().orElseThrow())
                        .map(this::mapAction)
                        .collect(Collectors.toList())
                )
                .build();
    }

    private PrivacyAction mapAction(ActionStats actionStats) {
        return ImmutablePrivacyAction.builder()
                .type(actionStats.getActionType())
                .submitted(Instant.ofEpochMilli(actionStats.getTimeStampSubmitted()))
                .build();
    }
}
