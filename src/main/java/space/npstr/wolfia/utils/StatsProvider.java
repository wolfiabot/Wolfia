/*
 * Copyright (C) 2017 Dennis Neufeld
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

package space.npstr.wolfia.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.game.definitions.Alignments;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static space.npstr.wolfia.utils.TextchatUtils.divide;
import static space.npstr.wolfia.utils.TextchatUtils.percentFormat;

/**
 * Created by napster on 10.06.17.
 * <p>
 * This class takes data out of the database and formats it in a presentable way
 */
public class StatsProvider {

    private static final Logger log = LoggerFactory.getLogger(StatsProvider.class);

    //SQL queries:
    //return all players a user ever was and join with data for the team and game
    private static final String userQuery = "SELECT player_id, nickname, role, total_postlength, total_posts, user_id, stats_team.team_id, alignment, is_winner, stats_team.name as team_name, stats_game.game_id, channel_id, channel_name, end_time, start_time, guild_id, guild_name, game_mode, game_type FROM public.stats_player\n" +
            "INNER JOIN public.stats_team ON (stats_player.team_id = stats_team.team_id)\n" +
            "INNER JOIN public.stats_game ON (stats_team.game_id = stats_game.game_id)\n" +
            "WHERE user_id = :userId";

    //return all SHOOT actions where userId pulled the trigger; join with team data for the target
    private static final String shatsQuery = "SELECT alignment, target FROM public.stats_action\n" +
            "INNER JOIN public.stats_player ON (stats_player.user_id = stats_action.target)\n" +
            "INNER JOIN public.stats_team ON (stats_team.team_id = stats_player.team_id)\n" +
            "INNER JOIN public.stats_game ON (stats_action.game_id = stats_game.game_id AND stats_team.game_id = stats_game.game_id)\n" +
            "WHERE (action_type = 'SHOOT' AND actor = :userId)";

    //selects the winning teams of all games in this guild
    private static final String guildQuery = "SELECT stats_game.game_id, alignment FROM public.stats_game\n" +
            "INNER JOIN public.stats_team ON (stats_team.game_id = stats_game.game_id)\n" +
            "WHERE (is_winner = true AND guild_id = :guildId)";

    @SuppressWarnings("unchecked")
    public static EmbedBuilder getGuildStats(final Guild g) {
        //get data out of the database
        final List<Map<String, Object>> gamesInGuildxWinningTeam = new ArrayList<>();
        final EntityManager em = Wolfia.dbManager.getEntityManager();
        try {
            final List<Object[]> result = em.createNativeQuery(guildQuery).setParameter("guildId", g.getIdLong()).getResultList();
            gamesInGuildxWinningTeam.addAll(DbUtils.asListOfMaps(result, DbUtils.getColumnNameToIndexMap(guildQuery, em)));
        } catch (final SQLException e) {
            log.error("SQL exception when querying stats for guild {}", g.getIdLong(), e);
        } finally {
            em.close();
        }

        //collect a bunch of values
        final long totalGames = gamesInGuildxWinningTeam.size();
        final long gamesWonByWolves = gamesInGuildxWinningTeam.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF).count();
        final long gamesWonByVillage = gamesInGuildxWinningTeam.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.VILLAGE).count();


        //add them to the embed
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(g.getName() + "'s Wolfia stats");
        eb.setThumbnail(g.getIconUrl());
        eb.addField("Total games played", totalGames + "", true);
        eb.addField("Players on average", "---", true);
        eb.addField("Win % for " + Emojis.WOLF, percentFormat(divide(gamesWonByWolves, totalGames)), true);
        eb.addField("Win % for " + Emojis.COWBOY, percentFormat(divide(gamesWonByVillage, totalGames)), true);
        return eb;
    }

    @SuppressWarnings("unchecked")
    public static EmbedBuilder getUserStats(final Member m) {
        //get data out of the database
        final List<Map<String, Object>> gamesByUser = new ArrayList<>();
        final List<Map<String, Object>> shatsByUser = new ArrayList<>();
        final EntityManager em = Wolfia.dbManager.getEntityManager();
        try {
            List<Object[]> result = em.createNativeQuery(userQuery).setParameter("userId", m.getUser().getIdLong()).getResultList();
            gamesByUser.addAll(DbUtils.asListOfMaps(result, DbUtils.getColumnNameToIndexMap(userQuery, em)));
            result = em.createNativeQuery(shatsQuery).setParameter("userId", m.getUser().getIdLong()).getResultList();
            shatsByUser.addAll(DbUtils.asListOfMaps(result, DbUtils.getColumnNameToIndexMap(shatsQuery, em)));
        } catch (final SQLException e) {
            log.error("SQL exception when querying stats for user {}", m.getUser().getIdLong(), e);
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

        //add them to the embed
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(m.getEffectiveName() + "'s Wolfia stats");
        eb.setThumbnail(m.getUser().getEffectiveAvatarUrl());
        eb.addField("Total games played", totalGamesByUser + "", true);
        eb.addField("Total win %", percentFormat(divide(gamesWon, totalGamesByUser)), true);
        eb.addField("Games as " + Emojis.WOLF, gamesAsWolf + "", true);
        eb.addField("Win % as " + Emojis.WOLF, percentFormat(divide(gamesWonAsWolf, gamesAsWolf)), true);
        eb.addField("Games as " + Emojis.COWBOY, gamesAsVillage + "", true);
        eb.addField("Win % as " + Emojis.COWBOY, percentFormat(divide(gamesWonAsVillage, gamesAsVillage)), true);
        eb.addField(Emojis.GUN + " fired", totalShatsByUser + "", true);
        eb.addField(Emojis.GUN + " accuracy", percentFormat(divide(wolvesShatted, totalShatsByUser)), true);
        eb.addField("Total posts written", totalPostsWritten + "", true);
        eb.addField("Total post length", totalPostsLength + "", true);
        eb.addField("∅ posts per game", ((long) divide(totalPostsWritten, totalGamesByUser)) + "", true);
        eb.addField("∅ post length", ((long) divide(totalPostsLength, totalPostsWritten)) + "", true);
        return eb;
    }


    //######### below stuff is keept around as inspiration for the future
//    private class BotStats {
//        int totalGamesPlayed;
//        double totalWinPercentageVillage;
//        double totalWinPercentageWolves;
//        double averagePlayersPerGame;
//
//        int popcornGamesPlayed;
//        double popcornWinPercentageVillage;
//        double popcornWinPercentageWolves;
//        double popcornAveragePlayersPerGame;
//    }
//
//    private class GuildStats {
//
//        private final long guildId;
//
//        public GuildStats(final long guildId) {
//            this.guildId = guildId;
//        }
//
//        @Override
//        public int hashCode() {
//            return Long.hashCode(this.guildId);
//        }
//
//        int totalGamesPlayed = 0; //all games ever played in this guild
//        int totalWinsVillage = 0; //village winsover all games
//        int totalWinsWolves = 0;  //wolve wins over all games
//        int totalPlayersPlayed; //average player size per game
//        Map<Integer, Integer> totalGamesWithPlayersize; //how many games were played with each player size
//        long totalGameLength; //average game lenth
//        long shortedGamePlayed; //the shortest game ever played on this server
//        long longestGamePlayed; //the longest game ever played on this server
//        double averagePostsPerGame; //average posts per game
//        double averagePostLengthsPerGame; //average post length per game
//
//        int popcornGamesPlayed; //all popcorn games ever played in this guild
//        double popcornWinPercentageVillage;//village win percentage for all popcorn games
//        double popcornWinPercentageWolves;//wolves win percentage over all games
//        double popcornAveragePlayersPerGame;//average popcorn game player size
//        Map<Integer, Integer> popcornGamesWithPlayersize; //how many popcorn games were played with each player size
//        double averageShootingAccuracy; //accuracy of wolves getting shot in this guild
//
//
//        double getVillageWinPercentage() {
//            return 100.0 * this.totalWinsVillage / this.totalGamesPlayed;
//        }
//
//        double getWolvesWinPercentage() {
//            return 100.0 * this.totalWinsWolves / this.totalGamesPlayed;
//        }
//
//        double getAveragePlayersPerGame() {
//            return 1.0 * this.totalPlayersPlayed / this.totalGamesPlayed;
//        }
//
//        long getAverageGameLength() {
//            return this.totalGameLength / this.totalGamesPlayed;
//        }
//    }
//
//    private class UserStats {
//        int totalGamesPlayed;
//        int totalGamesAsVillage;
//        int totalGamesAsWolf;
//        double totalWinPercentageAsVillage;
//        double totalWinPercentageAsWolf;
//        double averagePostsPerGame;
//        double averagePostLengthsPerGame;
//
//        int popcornGamesPlayed;
//        double popcornWinPercentageVillage;
//        double popcornWinPercentageWolves;
//        double shootingAccuracy;
//
//    }
}
