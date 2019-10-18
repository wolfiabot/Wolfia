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

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.commands.Context;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.domain.UserCache;
import space.npstr.wolfia.domain.settings.GuildSettings;
import space.npstr.wolfia.domain.settings.GuildSettingsService;
import space.npstr.wolfia.utils.discord.Emojis;

import java.util.List;
import java.util.Map;

import static space.npstr.wolfia.utils.discord.TextchatUtils.divide;
import static space.npstr.wolfia.utils.discord.TextchatUtils.percentFormat;

@Component
public class StatsRender {

    private final GuildSettingsService guildSettingsService;
    private final UserCache userCache;

    public StatsRender(GuildSettingsService guildSettingsService, UserCache userCache) {
        this.guildSettingsService = guildSettingsService;
        this.userCache = userCache;
    }

    public EmbedBuilder renderBotStats(Context context, Number averagePlayerSize, Map<Integer, List<Long>> collectedValues) {
        final EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        eb.setTitle("Wolfia stats:");
        context.getJda().asBot().getShardManager().getShardCache().stream().findAny()
                .map(shard -> shard.getSelfUser().getAvatarUrl())
                .ifPresent(eb::setThumbnail);

        //stats for all games:
        eb.addBlankField(false);
        final List<Long> values = collectedValues.remove(-1);
        eb.addField("Total games played", values.get(0) + "", true);
        eb.addField("∅ player size", String.format("%.2f", averagePlayerSize.doubleValue()), true);
        eb.addField("Win % for " + Emojis.WOLF, percentFormat(divide(values.get(1), values.get(0))), true);
        eb.addField("Win % for " + Emojis.COWBOY, percentFormat(divide(values.get(2), values.get(0))), true);
        //stats by playersize:
        eb.addBlankField(false);
        eb.addField("Stats by player size:", "", false);
        return addStatsPerPlayerSize(eb, collectedValues);
    }

    public EmbedBuilder renderGuildStats(Context context, long guildId, Number averagePlayerSize, final Map<Integer, List<Long>> collectedValues) {
        EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        final Guild guild = context.getJda().asBot().getShardManager().getGuildById(guildId);
        GuildSettings guildSettings = guild != null
                ? this.guildSettingsService.set(guild)
                : this.guildSettingsService.guild(guildId).getOrDefault();
        eb.setTitle(guildSettings.getName() + "'s Wolfia stats");
        eb.setThumbnail(guildSettings.getAvatarUrl().orElse(null));

        final long totalGames = collectedValues.get(-1).get(0);
        if (totalGames <= 0) {
            eb.setTitle(String.format("There have no games been played in the guild (id `%s`).", guildId));
            return eb;
        }

        //stats for all games in this guild:
        eb.addBlankField(false);
        final List<Long> values = collectedValues.remove(-1);
        eb.addField("Total games played", values.get(0) + "", true);
        eb.addField("∅ player size", String.format("%.2f", averagePlayerSize.doubleValue()), true);
        eb.addField("Win % for " + Emojis.WOLF, percentFormat(divide(values.get(1), values.get(0))), true);
        eb.addField("Win % for " + Emojis.COWBOY, percentFormat(divide(values.get(2), values.get(0))), true);
        //stats by playersize in this guild:
        eb.addBlankField(false);
        eb.addField("Stats by player size:", "", false);
        return addStatsPerPlayerSize(eb, collectedValues);
    }

    public EmbedBuilder renderUserStats(UserStats stats) {
        final EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        UserCache.Action userAction = this.userCache.user(stats.userId());
        eb.setTitle(userAction.getName() + "'s Wolfia stats");
        userAction.get()
                .map(User::getAvatarUrl)
                .ifPresent(eb::setThumbnail);

        if (stats.totalGames() <= 0) {
            eb.setTitle(String.format("User (id `%s`) hasn't played any games.", stats.userId()));
            return eb;
        }

        eb.addField("Total games played", stats.totalGames() + "", true);
        eb.addField("Total win %", percentFormat(divide(stats.gamesWon(), stats.totalGames())), true);
        eb.addField("Games as " + Emojis.WOLF, stats.gamesAsBaddie() + "", true);
        eb.addField("Win % as " + Emojis.WOLF, percentFormat(divide(stats.gamesWonAsBaddie(), stats.gamesAsBaddie())), true);
        eb.addField("Games as " + Emojis.COWBOY, stats.gamesAsGoodie() + "", true);
        eb.addField("Win % as " + Emojis.COWBOY, percentFormat(divide(stats.gamesWonAsGoodie(), stats.gamesAsGoodie())), true);
        eb.addField(Emojis.GUN + " fired", stats.totalShots() + "", true);
        eb.addField(Emojis.GUN + " accuracy", percentFormat(divide(stats.wolvesShot(), stats.totalShots())), true);
        eb.addField("Total posts written", stats.totalPosts() + "", true);
        eb.addField("Total post length", stats.totalPostLength() + "", true);
        eb.addField("∅ posts per game", ((long) divide(stats.totalPosts(), stats.totalGames())) + "", true);
        eb.addField("∅ post length", ((long) divide(stats.totalPostLength(), stats.totalPosts())) + "", true);
        return eb;
    }

    private static EmbedBuilder addStatsPerPlayerSize(final EmbedBuilder eb, final Map<Integer, List<Long>> collectedValues) {
        for (final Map.Entry<Integer, List<Long>> entry : collectedValues.entrySet()) {
            final int playerSize = entry.getKey();
            final List<Long> values = entry.getValue();
            String content = Emojis.WOLF + " win " + percentFormat(divide(values.get(1), values.get(0)));
            content += "\n" + Emojis.COWBOY + " win " + percentFormat(divide(values.get(2), values.get(0)));
            eb.addField(values.get(0) + " games with " + playerSize + " players", content, true);
        }
        return eb;
    }
}
