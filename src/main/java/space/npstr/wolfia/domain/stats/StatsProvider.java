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

package space.npstr.wolfia.domain.stats;

import org.springframework.stereotype.Component;
import space.npstr.wolfia.db.ColumnMapper;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.game.definitions.Alignments;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 10.06.17.
 * <p>
 * This class takes data out of the database and formats it in a presentable way
 */
@Component
public class StatsProvider {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StatsProvider.class);

    //SQL queries:
    private static class Queries {

        private static class Bot {
            //average player size bot wide
            private static final String AVERAGE_PLAYERS_SIZE = "SELECT AVG(stats_game.player_size) FROM public.stats_game";

            //select teams + games
            private static final String TEAMS = "SELECT stats_game.game_id, stats_team.alignment FROM public.stats_game\n" +
                    "INNER JOIN public.stats_team ON (stats_team.game_id = stats_game.game_id)";

            //select winning teams of all games
            private static final String WINNING_TEAMS = TEAMS +
                    "\nWHERE (stats_team.is_winner = TRUE)";

            //select winning teams of games of a certain size
            private static final String WINNING_TEAMS_FOR_PLAYER_SIZE = TEAMS +
                    "\nWHERE (stats_team.is_winner = TRUE AND stats_game.player_size = :playerSize)";

            //select all unique player sizes for games that are recorded
            private static final String DISTINCT_PLAYER_SIZES = "SELECT DISTINCT stats_game.player_size FROM public.stats_game";
        }

        private static class Guild {

            //average player size for a guild
            private static final String AVERAGE_PLAYERS_SIZE = Bot.AVERAGE_PLAYERS_SIZE +
                    "\nWHERE (stats_game.guild_id = :guildId)";

            //select winning teams of all games in this guild
            private static final String WINNING_TEAMS = Bot.TEAMS +
                    "\nWHERE (stats_game.guild_id = :guildId AND stats_team.is_winner = TRUE)";

            //select winning teams of games of a certain size in this guild
            private static final String WINNING_TEAMS_FOR_PLAYER_SIZE = Bot.TEAMS +
                    "\nWHERE (stats_game.guild_id = :guildId AND stats_team.is_winner = TRUE AND stats_game.player_size = :playerSize)";

            private static final String DISTINCT_PLAYER_SIZES = Bot.DISTINCT_PLAYER_SIZES +
                    "\nWHERE (stats_game.guild_id = :guildId)";

        }

        private static class User {
            //return all players a user ever was and join with data for the team and game
            private static final String GENERAL =
                    "SELECT stats_player.total_postlength, stats_player.total_posts, stats_player.alignment, stats_team.is_winner FROM public.stats_player\n" +
                            "INNER JOIN public.stats_team ON (stats_player.team_id = stats_team.team_id)\n" +
                            "WHERE stats_player.user_id = :userId";

            //return all SHOOT actions where userId pulled the trigger; join with team data for the target
            private static final String SHATS =
                    "SELECT stats_player.alignment, stats_action.target FROM public.stats_action\n" +
                            "INNER JOIN public.stats_player ON (stats_player.user_id = stats_action.target)\n" +
                            "INNER JOIN public.stats_team ON (stats_team.team_id = stats_player.team_id)\n" +
                            "INNER JOIN public.stats_game ON (stats_action.game_id = stats_game.game_id AND stats_team.game_id = stats_game.game_id)\n" +
                            "WHERE (stats_action.action_type = 'SHOOT' AND stats_action.actor = :userId)";
        }
    }

    private final Database database;

    public StatsProvider(Database database) {
        this.database = database;
    }

    //this should be rather similar to getGuildStats
    @SuppressWarnings("unchecked")
    public BotStats getBotStats() {
        //get data out of the database
        BigDecimal averagePlayerSize = new BigDecimal(0);
        final Map<Integer, List<Map<String, Object>>> gamesxWinningTeamByPlayerSize = new LinkedHashMap<>();//linked to preserve sorting

        final EntityManager em = this.database.getEntityManager();
        try {
            em.getTransaction().begin();
            averagePlayerSize = (BigDecimal) em.createNativeQuery(Queries.Bot.AVERAGE_PLAYERS_SIZE).getSingleResult();
            if (averagePlayerSize == null) averagePlayerSize = new BigDecimal(0);

            //get winning teams by player sizes
            List<Object[]> result = em.createNativeQuery(Queries.Bot.WINNING_TEAMS).getResultList();
            //add total stats with size -1; there better not by any -1 sized entries in the database
            gamesxWinningTeamByPlayerSize.put(-1, ColumnMapper.asListOfMaps(result, ColumnMapper.getColumnNameToIndexMap(Queries.Bot.WINNING_TEAMS, em)));
            final List<Integer> existingPlayerSizes = em.createNativeQuery(Queries.Bot.DISTINCT_PLAYER_SIZES).getResultList();
            Collections.sort(existingPlayerSizes);
            for (final int playerSize : existingPlayerSizes) {
                if (playerSize < 1) {
                    //skip and log about weird player sizes in the db
                    log.error("Found unexpected player size {} in the database with query '{}'", playerSize, Queries.Bot.DISTINCT_PLAYER_SIZES);
                    continue;
                }
                result = em.createNativeQuery(Queries.Bot.WINNING_TEAMS_FOR_PLAYER_SIZE).setParameter("playerSize", playerSize).getResultList();
                gamesxWinningTeamByPlayerSize.put(playerSize, ColumnMapper.asListOfMaps(result, ColumnMapper.getColumnNameToIndexMap(Queries.Bot.WINNING_TEAMS_FOR_PLAYER_SIZE, em)));
            }
            em.getTransaction().commit();
        } catch (final SQLException e) {
            log.error("SQL exception when querying bot stats", e);
        } finally {
            em.close();
        }

        //collect a bunch of values
        final Map<Integer, List<Long>> collectedValues = collectValues(gamesxWinningTeamByPlayerSize);

        return ImmutableBotStats.builder()
                .averagePlayerSize(averagePlayerSize)
                .putAllCollectedValues(collectedValues)
                .build();
    }


    @SuppressWarnings("unchecked")
    public GuildStats getGuildStats(final long guildId) {
        //get data out of the database
        BigDecimal averagePlayerSize = new BigDecimal(0);
        final Map<Integer, List<Map<String, Object>>> gamesxWinningTeamInGuildByPlayerSize = new LinkedHashMap<>();//linked to preserve sorting

        final EntityManager em = this.database.getEntityManager();
        try {
            em.getTransaction().begin();
            averagePlayerSize = (BigDecimal) em.createNativeQuery(Queries.Guild.AVERAGE_PLAYERS_SIZE).setParameter("guildId", guildId).getSingleResult();
            if (averagePlayerSize == null) averagePlayerSize = new BigDecimal(0);

            //get winning teams by player sizes
            List<Object[]> result = em.createNativeQuery(Queries.Guild.WINNING_TEAMS).setParameter("guildId", guildId).getResultList();
            //add total stats with size -1; there better not by any -1 sized entries in the database
            gamesxWinningTeamInGuildByPlayerSize.put(-1, ColumnMapper.asListOfMaps(result, ColumnMapper.getColumnNameToIndexMap(Queries.Guild.WINNING_TEAMS, em)));
            final List<Integer> existingPlayerSizes = em.createNativeQuery(Queries.Guild.DISTINCT_PLAYER_SIZES).setParameter("guildId", guildId).getResultList();
            Collections.sort(existingPlayerSizes);
            for (final int playerSize : existingPlayerSizes) {
                if (playerSize < 1) {
                    //skip and log about weird player sizes in the db
                    log.error("Found unexpected player size {} in the database with query '{}'", playerSize, Queries.Guild.DISTINCT_PLAYER_SIZES);
                    continue;
                }
                result = em.createNativeQuery(Queries.Guild.WINNING_TEAMS_FOR_PLAYER_SIZE).setParameter("guildId", guildId).setParameter("playerSize", playerSize).getResultList();
                gamesxWinningTeamInGuildByPlayerSize.put(playerSize, ColumnMapper.asListOfMaps(result, ColumnMapper.getColumnNameToIndexMap(Queries.Guild.WINNING_TEAMS_FOR_PLAYER_SIZE, em)));
            }
            em.getTransaction().commit();
        } catch (final SQLException e) {
            log.error("SQL exception when querying stats for guild {}", guildId, e);
        } finally {
            em.close();
        }

        //collect a bunch of values
        final Map<Integer, List<Long>> collectedValues = collectValues(gamesxWinningTeamInGuildByPlayerSize);

        return ImmutableGuildStats.builder()
                .guildId(guildId)
                .averagePlayerSize(averagePlayerSize)
                .putAllCollectedValues(collectedValues)
                .build();
    }

    @SuppressWarnings("unchecked")
    public UserStats getUserStats(final long userId) {
        //get data out of the database
        final List<Map<String, Object>> gamesByUser = new ArrayList<>();
        final List<Map<String, Object>> shatsByUser = new ArrayList<>();
        final EntityManager em = this.database.getEntityManager();
        try {
            em.getTransaction().begin();
            List<Object[]> result = em.createNativeQuery(Queries.User.GENERAL).setParameter("userId", userId).getResultList();
            gamesByUser.addAll(ColumnMapper.asListOfMaps(result, ColumnMapper.getColumnNameToIndexMap(Queries.User.GENERAL, em)));
            result = em.createNativeQuery(Queries.User.SHATS).setParameter("userId", userId).getResultList();
            shatsByUser.addAll(ColumnMapper.asListOfMaps(result, ColumnMapper.getColumnNameToIndexMap(Queries.User.SHATS, em)));
            em.getTransaction().commit();
        } catch (final SQLException e) {
            log.error("SQL exception when querying stats for user {}", userId, e);
        } finally {
            em.close();
        }

        //collect a bunch of values
        final long totalGamesByUser = gamesByUser.size();
        final long gamesWon = gamesByUser.stream().filter(map -> (boolean) map.get("is_winner")).count();
        final long gamesAsWolf = gamesByUser.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF).count();
        final long gamesAsVillage = gamesByUser.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.VILLAGE).count();
        final long gamesWonAsWolf = gamesByUser.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF)
                .filter(map -> (boolean) map.get("is_winner"))
                .count();
        final long gamesWonAsVillage = gamesByUser.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.VILLAGE)
                .filter(map -> (boolean) map.get("is_winner"))
                .count();
        final long totalPostsWritten = gamesByUser.stream().mapToInt(map -> (int) map.get("total_posts")).sum();
        final long totalPostsLength = gamesByUser.stream().mapToInt(map -> (int) map.get("total_postlength")).sum();
        final long totalShatsByUser = shatsByUser.size();
        final long wolvesShatted = shatsByUser.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF).count();

        return ImmutableUserStats.builder()
                .userId(userId)
                .totalGames(totalGamesByUser)
                .gamesWon(gamesWon)
                .gamesAsBaddie(gamesAsWolf)
                .gamesWonAsBaddie(gamesWonAsWolf)
                .gamesAsGoodie(gamesAsVillage)
                .gamesWonAsGoodie(gamesWonAsVillage)
                .totalShots(totalShatsByUser)
                .wolvesShot(wolvesShatted)
                .totalPosts(totalPostsWritten)
                .totalPostLength(totalPostsLength)
                .build();
    }

    //todo introduce a proper data structure for this
    private static Map<Integer, List<Long>> collectValues(final Map<Integer, List<Map<String, Object>>> input) {
        final Map<Integer, List<Long>> result = new LinkedHashMap<>();//linked to preserve sorting
        for (final Map.Entry<Integer, List<Map<String, Object>>> entry : input.entrySet()) {
            final int playerSize = entry.getKey();
            final List<Map<String, Object>> gamesxWinningTeam = entry.getValue();
            final long totalGames = gamesxWinningTeam.size();
            final long gamesWonByWolves = gamesxWinningTeam.stream()
                    .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF).count();
            final long gamesWonByVillage = gamesxWinningTeam.stream()
                    .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.VILLAGE).count();

            result.put(playerSize, Arrays.asList(totalGames, gamesWonByWolves, gamesWonByVillage));
        }
        return result;
    }
}
