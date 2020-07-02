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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.commands.Context;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.domain.UserCache;
import space.npstr.wolfia.domain.settings.GuildSettings;
import space.npstr.wolfia.domain.settings.GuildSettingsService;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Item;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import static java.util.Objects.requireNonNull;
import static space.npstr.wolfia.game.Player.UNKNOWN_NAME;
import static space.npstr.wolfia.utils.discord.TextchatUtils.divide;
import static space.npstr.wolfia.utils.discord.TextchatUtils.percentFormat;

@Component
public class StatsRender {

    private static final Logger log = LoggerFactory.getLogger(StatsRender.class);

    private final GuildSettingsService guildSettingsService;
    private final UserCache userCache;

    public StatsRender(GuildSettingsService guildSettingsService, UserCache userCache) {
        this.guildSettingsService = guildSettingsService;
        this.userCache = userCache;
    }

    public EmbedBuilder renderBotStats(Context context, BotStats botstats) {
        final EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        eb.setTitle("Wolfia stats:");
        ShardManager shardManager = context.getJda().getShardManager();
        requireNonNull(shardManager).getShardCache().stream().findAny()
                .map(shard -> shard.getSelfUser().getAvatarUrl())
                .ifPresent(eb::setThumbnail);

        // stats for all games
        eb.addBlankField(false);
        final WinStats winStats = botstats.totalWinStats();
        eb.addField("Total games played", winStats.totalGames() + "", true);
        eb.addField("∅ player size", String.format("%.2f", botstats.averagePlayerSize().doubleValue()), true);

        double baddieWinPercentage = divide(winStats.baddieWins(), winStats.totalGames());
        double goodieWinPercentage = divide(winStats.goodieWins(), winStats.totalGames());
        eb.addField("Win % for " + Emojis.WOLF, percentFormat(baddieWinPercentage), true);
        eb.addField("Win % for " + Emojis.COWBOY, percentFormat(goodieWinPercentage), true);

        // stats by playersize
        eb.addBlankField(false);
        eb.addField("Stats by player size:", "", false);
        return addStatsPerPlayerSize(eb, botstats.winStatsByPlayerSize());
    }

    public EmbedBuilder renderGuildStats(Context context, GuildStats stats) {
        EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        ShardManager shardManager = context.getJda().getShardManager();
        final Guild guild = requireNonNull(shardManager).getGuildById(stats.guildId());
        GuildSettings guildSettings = guild != null
                ? this.guildSettingsService.set(guild)
                : this.guildSettingsService.guild(stats.guildId()).getOrDefault();
        eb.setTitle(guildSettings.getName() + "'s Wolfia stats");
        eb.setThumbnail(guildSettings.getAvatarUrl().orElse(null));

        WinStats winStats = stats.totalWinStats();
        final long totalGames = winStats.totalGames();
        if (totalGames <= 0) {
            eb.setTitle(String.format("There have no games been played in the guild (id `%s`).", stats.guildId()));
            return eb;
        }

        // stats for all games in this guild
        eb.addBlankField(false);
        eb.addField("Total games played", totalGames + "", true);
        eb.addField("∅ player size", String.format("%.2f", stats.averagePlayerSize().doubleValue()), true);

        double baddieWinPercentage = divide(winStats.baddieWins(), winStats.totalGames());
        double goodieWinPercentage = divide(winStats.goodieWins(), winStats.totalGames());
        eb.addField("Win % for " + Emojis.WOLF, percentFormat(baddieWinPercentage), true);
        eb.addField("Win % for " + Emojis.COWBOY, percentFormat(goodieWinPercentage), true);

        // stats by playersize in this guild
        eb.addBlankField(false);
        eb.addField("Stats by player size:", "", false);
        return addStatsPerPlayerSize(eb, stats.winStatsByPlayerSize());
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


    public EmbedBuilder renderGameStats(GameStats stats) {
        final NiceEmbedBuilder eb = NiceEmbedBuilder.defaultBuilder();

        long gameId = stats.getGameId().orElseThrow();

        //1. post summary like game, mode, players, roles
        eb.setTitle("**Game #" + gameId + "**");
        eb.setDescription(stats.getGameType().textRep + " " + stats.getGameMode().textRep);
        eb.addField("Game started", TextchatUtils.toUtcTime(stats.getStartTime()), true);

        stats.getStartingTeams().forEach(team ->
                eb.addField(stats.getGameType() == Games.POPCORN ? team.getAlignment().textRepWW : team.getAlignment().textRepMaf,
                        team.getPlayers().stream()
                                .map(player -> "`" + player.getNickname() + "`")
                                .collect(Collectors.joining(", ")),
                        true)
        );

        //2. post the actions
        final List<ActionStats> sortedActions = new ArrayList<>(stats.getActions());
        sortedActions.sort(Comparator.comparingLong(ActionStats::getTimeStampSubmitted));
        final String fieldTitle = "Actions";
        final NiceEmbedBuilder.ChunkingField actionsField = new NiceEmbedBuilder.ChunkingField(fieldTitle, false);
        for (final ActionStats action : sortedActions) {
            final String actionStr = renderActionStats(action);
            actionsField.add(actionStr, true);
        }
        eb.addField(actionsField);

        //3. post the winners
        eb.addField("Game ended", TextchatUtils.toUtcTime(stats.getEndTime()), true);
        eb.addField("Game length", TextchatUtils.formatMillis(stats.getEndTime() - stats.getStartTime()), true);

        final String winText;
        final Optional<TeamStats> winners = stats.getStartingTeams().stream().filter(TeamStats::isWinner).findFirst();
        if (winners.isEmpty()) {
            //shouldn't happen lol
            log.error("Game #{} has no winning team in the data", gameId);
            winText = "Game has no winning team " + Emojis.WOLFTHINK + "\nReplay must be borked. Error has been reported.";
        } else {
            final TeamStats winningTeam = winners.get();
            String flavouredTeamName = winningTeam.getAlignment().textRepMaf;
            if (stats.getGameType() == Games.POPCORN) flavouredTeamName = winningTeam.getAlignment().textRepWW;
            winText = "**Team " + flavouredTeamName + " wins the game!**";
        }
        eb.addField("Winners", winText, true);
        return eb;
    }

    public String renderActionStats(ActionStats actionStats) {

        GameStats game = actionStats.getGame();
        long gameId = game.getGameId().orElseThrow();
        //how much time since game started
        String result = "`" + (TextchatUtils.formatMillis(actionStats.getTimeStampHappened() - game.getStartTime())) + "` ";
        switch (actionStats.getActionType()) {

            case GAMESTART:
                result += String.format("%s: Game **#%s** starts.", Emojis.VIDEO_GAME, gameId);
                break;
            case GAMEEND:
                result += String.format("%s: Game **#%s** ends.", Emojis.END, gameId);
                break;
            case DAYSTART:
                result += String.format("%s: Day **%s** starts.", Emojis.SUNNY, actionStats.getCycle());
                break;
            case DAYEND:
                result += String.format("%s: Day **%s** ends.", Emojis.CITY_SUNSET_SUNRISE, actionStats.getCycle());
                break;
            case NIGHTSTART:
                result += String.format("%s: Night **%s** starts.", Emojis.FULL_MOON, actionStats.getCycle());
                break;
            case NIGHTEND:
                result += String.format("%s: Night **%s** ends.", Emojis.CITY_SUNSET_SUNRISE, actionStats.getCycle());
                break;
            case BOTKILL:
                result += String.format("%s: %s botkilled.", Emojis.SKULL, getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case MODKILL:
                result += String.format("%s: %s modkilled.", Emojis.COFFIN, getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case DEATH:
                result += String.format("%s: %s dies.", Emojis.RIP, getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case VOTELYNCH:
                result += String.format("%s: %s votes to lynch %s.", Emojis.BALLOT_BOX, getFormattedNickFromStats(game, actionStats.getActor()), getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case LYNCH:
                result += String.format("%s: %s is lynched", Emojis.FIRE, getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case VOTENIGHTKILL:
                result += String.format("%s: %s votes to night kill %s.", Emojis.BALLOT_BOX, getFormattedNickFromStats(game, actionStats.getActor()), getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case CHECK:
                result += String.format("%s: %s checks alignment of %s.", Emojis.MAGNIFIER, getFormattedNickFromStats(game, actionStats.getActor()), getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case SHOOT:
                result += String.format("%s: %s shoots %s.", Emojis.GUN, getFormattedNickFromStats(game, actionStats.getActor()), getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case VOTEGUN:
                result += String.format("%s: %s votes to give %s the %s.", Emojis.BALLOT_BOX, getFormattedNickFromStats(game, actionStats.getActor()), getFormattedNickFromStats(game, actionStats.getTarget()), Emojis.GUN);
                break;
            case GIVEGUN:
                result += String.format("%s: %s receives the gun", Emojis.GUN, getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case GIVE_PRESENT:
                result += String.format("%s: %s gives %s a present", Item.ItemType.PRESENT, getFormattedNickFromStats(game, actionStats.getActor()), getFormattedNickFromStats(game, actionStats.getTarget()));
                break;
            case OPEN_PRESENT:
                result += String.format("%s: %s opens a present and receives a %s", Item.ItemType.PRESENT, getFormattedNickFromStats(game, actionStats.getTarget()), Item.ItemType.valueOf(actionStats.getAdditionalInfo()));
                break;
            default:
                throw new IllegalArgumentException("Encountered an action that is not defined/has no text representation: " + actionStats.getActionType());
        }

        return result;
    }

    private static EmbedBuilder addStatsPerPlayerSize(final EmbedBuilder eb, List<WinStats> winStatsList) {
        List<WinStats> sortedWinStats = new ArrayList<>(winStatsList);
        sortedWinStats.sort(Comparator.comparingInt(WinStats::playerSize));

        for (WinStats winStats : sortedWinStats) {
            double baddieWinPercentage = divide(winStats.baddieWins(), winStats.totalGames());
            double goodieWinPercentage = divide(winStats.goodieWins(), winStats.totalGames());
            String content = Emojis.WOLF + " win " + percentFormat(baddieWinPercentage);
            content += "\n" + Emojis.COWBOY + " win " + percentFormat(goodieWinPercentage);
            eb.addField(winStats.totalGames() + " games with " + winStats.playerSize() + " players",
                    content, true);
        }
        return eb;
    }


    private String getFormattedNickFromStats(GameStats gameStats, final long userId) {
        String baddieEmoji = Emojis.SPY;
        if (gameStats.getGameType() == Games.POPCORN) baddieEmoji = Emojis.WOLF;
        for (final TeamStats team : gameStats.getStartingTeams()) {
            for (final PlayerStats player : team.getPlayers()) {
                if (player.getUserId() == userId)
                    return "`" + player.getNickname() + "` " + (player.getAlignment() == Alignments.VILLAGE ? Emojis.COWBOY : baddieEmoji);
            }
        }
        final String message = String.format("No such player %s in this game %s", userId, gameStats.getGameId().orElseThrow());
        log.error(message, new IllegalArgumentException(message));
        return UNKNOWN_NAME;
    }

}
