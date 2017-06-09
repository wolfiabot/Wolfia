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

package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.utils.DbUtils;
import space.npstr.wolfia.utils.Emojis;
import space.npstr.wolfia.utils.IllegalGameStateException;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 08.06.17.
 */
public class StatsCommand implements ICommand {

    public static final String COMMAND = "stats";

    private static final Logger log = LoggerFactory.getLogger(StatsCommand.class);

    @Override
    public String help() {
        return "todo";//todo
    }

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {

        final Message m = commandInfo.event.getMessage();

        if (m.getMentionedUsers().size() < 1) {
            Wolfia.handleOutputEmbed(m.getChannel(), outputUserStats(commandInfo.event.getMember()).build());
            return;
        }

        for (final User u : m.getMentionedUsers()) {
            if (!m.getGuild().isMember(u)) continue; //skip mentioned non-members
            final MessageEmbed e = outputUserStats(m.getGuild().getMember(u)).build();
            Wolfia.handleOutputEmbed(m.getChannel(), e);
        }

//        for (final Channel c : m.getMentionedChannels()) {
//            outputChannelStats(c);
//        }
//
//        outputGuildStats(m.getGuild());


    }

    private final String query1 = "SELECT player_id, nickname, role, total_postlength, total_posts, user_id, stats_team.team_id, alignment, is_winner, stats_team.name as team_name, stats_game.game_id, channel_id, channel_name, end_time, start_time, guild_id, guild_name, game_mode, game_type FROM public.stats_player\n" +
            "INNER JOIN public.stats_team ON (stats_player.team_id = stats_team.team_id)\n" +
            "INNER JOIN public.stats_game ON (stats_team.game_id = stats_game.game_id)\n" +
            "WHERE user_id = :userId";

    private final String shats = "SELECT alignment, target FROM public.stats_action\n" +
            "INNER JOIN public.stats_player ON (stats_player.user_id = stats_action.target)\n" +
            "INNER JOIN public.stats_team ON (stats_team.team_id = stats_player.team_id)\n" +
            "INNER JOIN public.stats_game ON (stats_action.game_id = stats_game.game_id AND stats_team.game_id = stats_game.game_id)\n" +
            "WHERE (actor = :userId AND action_type = 'SHOOT')";

    private EmbedBuilder outputUserStats(final Member m) {
        //get data out of the database
        final List<Map<String, Object>> res = new ArrayList<>();
        final List<Map<String, Object>> shats = new ArrayList<>();
        final EntityManager em = Wolfia.dbManager.getEntityManager();
        try {
            //noinspection unchecked
            List<Object[]> result = em.createNativeQuery(this.query1).setParameter("userId", m.getUser().getIdLong()).getResultList();
            res.addAll(DbUtils.asListOfMaps(result, DbUtils.getColumnNameToIndexMap(this.query1, em)));

            //noinspection unchecked
            result = em.createNativeQuery(this.shats).setParameter("userId", m.getUser().getIdLong()).getResultList();
            shats.addAll(DbUtils.asListOfMaps(result, DbUtils.getColumnNameToIndexMap(this.shats, em)));

        } catch (final SQLException e) {
            log.error("Some SQL exception", e);
        } finally {
            em.close();
        }


        //collect a bunch of values
        final long gamesWon = res.stream().filter(map -> (boolean) map.get("is_winner")).count();
        final long gamesAsWolf = res.stream().filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF).count();
        final long gamesAsVillage = res.stream().filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.VILLAGE).count();
        final long gamesWonAsWolf = res.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF)
                .filter(map -> (boolean) map.get("is_winner"))
                .count();
        final long gamesWonAsVillage = res.stream()
                .filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.VILLAGE)
                .filter(map -> (boolean) map.get("is_winner"))
                .count();
        final long totalPostsWritten = res.stream().mapToInt(map -> (int) map.get("total_posts")).sum();
        final long totalPostsLength = res.stream().mapToInt(map -> (int) map.get("total_postlength")).sum();
        final long wolvesShatted = shats.stream().filter(map -> Alignments.valueOf((String) map.get("alignment")) == Alignments.WOLF)
                .count();

        //add them to the embed
        final NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(2);
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(m.getEffectiveName() + "'s Wolfia stats");
        eb.setThumbnail(m.getUser().getEffectiveAvatarUrl());
        eb.addField("Total games played", res.size() + "", true);
        eb.addField("Total win %", nf.format(1.0 * gamesWon / res.size()), true);
        eb.addField("Games as " + Emojis.WOLF, gamesAsWolf + "", true);
        eb.addField("Win % as " + Emojis.WOLF, nf.format(1.0 * gamesWonAsWolf / gamesAsWolf), true);
        eb.addField("Games as " + Emojis.COWBOY, gamesAsVillage + "", true);
        eb.addField("Win % as " + Emojis.COWBOY, nf.format(1.0 * gamesWonAsVillage / gamesAsVillage), true);
        eb.addField(Emojis.GUN + " fired", shats.size() + "", true);
        eb.addField(Emojis.GUN + " accuracy", nf.format(1.0 * wolvesShatted / shats.size()), true);
        eb.addField("Total posts written", totalPostsWritten + "", true);
        eb.addField("Total post length", totalPostsLength + "", true);
        eb.addField("∅ posts per game", totalPostsWritten / res.size() + "", true);
        eb.addField("∅ post length", totalPostsLength / totalPostsWritten + "", true);
        return eb;
    }

    private void outputChannelStats(final Channel c) {

    }

    private void outputGuildStats(final Guild g) {
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setThumbnail(g.getIconUrl());
    }
}
