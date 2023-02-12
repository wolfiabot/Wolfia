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

package space.npstr.wolfia.domain.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.game.definitions.Alignments;

/**
 * Collect various stats from the stats tables.
 */
@Component
public class StatsProvider {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StatsProvider.class);

    private final StatsRepository repository;

    public StatsProvider(StatsRepository repository) {
        this.repository = repository;
    }

    //this should be rather similar to getGuildStats
    public BotStats getBotStats() {
        BigDecimal averagePlayerSize = this.repository.getAveragePlayerSize().toCompletableFuture().join();

        long baddieWins = this.repository.countAlignmentWins(Alignments.WOLF).toCompletableFuture().join();
        long goodieWins = this.repository.countAlignmentWins(Alignments.VILLAGE).toCompletableFuture().join();

        long totalGames = baddieWins + goodieWins; // correct for now, may change in the future

        WinStats totalWinStats = new WinStats(-1, totalGames, goodieWins, baddieWins);

        List<WinStats> winStats = new ArrayList<>();
        Set<Integer> playerSizes = this.repository.getDistinctPlayerSizes().toCompletableFuture().join();
        for (int playerSize : playerSizes) {
            if (playerSize < 1) {
                //skip and log about weird player sizes in the db
                log.error("Found unexpected player size {} in the database", playerSize);
                continue;
            }
            baddieWins = this.repository.countAlignmentWinsForPlayerSize(Alignments.WOLF, playerSize)
                    .toCompletableFuture().join();
            goodieWins = this.repository.countAlignmentWinsForPlayerSize(Alignments.VILLAGE, playerSize)
                    .toCompletableFuture().join();

            totalGames = baddieWins + goodieWins;
            winStats.add(new WinStats(playerSize, totalGames, goodieWins, baddieWins));
        }
        return new BotStats(averagePlayerSize, totalWinStats, winStats);
    }


    public GuildStats getGuildStats(final long guildId) {

        BigDecimal averagePlayerSize = this.repository.getAveragePlayerSizeInGuild(guildId).toCompletableFuture().join();

        long baddieWins = this.repository.countAlignmentWinsInGuild(Alignments.WOLF, guildId)
                .toCompletableFuture().join();
        long goodieWins = this.repository.countAlignmentWinsInGuild(Alignments.VILLAGE, guildId)
                .toCompletableFuture().join();

        long totalGames = baddieWins + goodieWins;

        WinStats totalWinStats = new WinStats(-1, totalGames, goodieWins, baddieWins);

        List<WinStats> winStats = new ArrayList<>();
        Set<Integer> playerSizes = this.repository.getDistinctPlayerSizesInGuild(guildId).toCompletableFuture().join();
        for (int playerSize : playerSizes) {
            if (playerSize < 1) {
                //skip and log about weird player sizes in the db
                log.error("Found unexpected player size {} in the database", playerSize);
                continue;
            }

            baddieWins = this.repository.countAlignmentWinsForPlayerSizeInGuild(Alignments.WOLF, playerSize, guildId)
                    .toCompletableFuture().join();
            goodieWins = this.repository.countAlignmentWinsForPlayerSizeInGuild(Alignments.VILLAGE, playerSize, guildId)
                    .toCompletableFuture().join();

            totalGames = baddieWins + goodieWins;
            winStats.add(new WinStats(playerSize, totalGames, goodieWins, baddieWins));
        }

        return new GuildStats(guildId, averagePlayerSize, totalWinStats, winStats);
    }

    //TODO some improvement is possible here by reducing the amount of individual sql queries run as well as the amount
    // of data fetched
    public UserStats getUserStats(final long userId) {
        List<GeneralUserStats> games = this.repository.getGeneralUserStats(userId).toCompletableFuture().join();
        List<String> shots = this.repository.getUserShots(userId).toCompletableFuture().join();

        final long totalGamesByUser = games.size();
        final long gamesWon = games.stream().filter(GeneralUserStats::isWinner).count();
        final long gamesAsWolf = games.stream()
                .filter(stats -> Alignments.valueOf(stats.getAlignment()) == Alignments.WOLF).count();
        final long gamesAsVillage = games.stream()
                .filter(stats -> Alignments.valueOf(stats.getAlignment()) == Alignments.VILLAGE).count();
        final long gamesWonAsWolf = games.stream()
                .filter(stats -> Alignments.valueOf(stats.getAlignment()) == Alignments.WOLF)
                .filter(GeneralUserStats::isWinner)
                .count();
        final long gamesWonAsVillage = games.stream()
                .filter(stats -> Alignments.valueOf(stats.getAlignment()) == Alignments.VILLAGE)
                .filter(GeneralUserStats::isWinner)
                .count();
        final long totalPostsWritten = games.stream().mapToLong(GeneralUserStats::getPosts).sum();
        final long totalPostsLength = games.stream().mapToLong(GeneralUserStats::getPostLength).sum();
        final long totalShatsByUser = shots.size();
        final long wolvesShatted = shots.stream()
                .filter(alignment -> Alignments.valueOf(alignment) == Alignments.WOLF).count();

        return new UserStats(
                userId,
                totalGamesByUser,
                gamesWon,
                gamesAsWolf,
                gamesWonAsWolf,
                gamesAsVillage,
                gamesWonAsVillage,
                totalShatsByUser,
                wolvesShatted,
                totalPostsWritten,
                totalPostsLength
        );
    }

    public Optional<GameStats> getGameStats(long gameId) {
        return this.repository.findGameStats(gameId)
                .toCompletableFuture().join();
    }
}
