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

package space.npstr.wolfia.domain.stats;

import io.prometheus.client.Summary;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.RecordMapper;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.gen.tables.records.StatsActionRecord;
import space.npstr.wolfia.db.gen.tables.records.StatsPlayerRecord;
import space.npstr.wolfia.db.gen.tables.records.StatsTeamRecord;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.system.metrics.MetricsRegistry;

import javax.annotation.CheckReturnValue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.jooq.impl.DSL.avg;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.defaultValue;
import static space.npstr.wolfia.db.gen.Tables.STATS_ACTION;
import static space.npstr.wolfia.db.gen.Tables.STATS_GAME;
import static space.npstr.wolfia.db.gen.Tables.STATS_PLAYER;
import static space.npstr.wolfia.db.gen.Tables.STATS_TEAM;

@Repository
public class StatsRepository {

    private final AsyncDbWrapper wrapper;

    public StatsRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<BigDecimal> getAveragePlayerSize() {
        Summary.Child timer = MetricsRegistry.queryTime.labels("getAveragePlayerSize");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .select(avg(STATS_GAME.PLAYER_SIZE))
                .from(STATS_GAME)
                .fetchOptional() // SQL AVG may return null for empty sets
                .map(Record1::component1)
                .orElse(BigDecimal.ZERO)
        ));
    }

    @CheckReturnValue
    public CompletionStage<BigDecimal> getAveragePlayerSizeInGuild(long guildId) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("getAveragePlayerSizeInGuild");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .select(avg(STATS_GAME.PLAYER_SIZE))
                .from(STATS_GAME)
                .where(STATS_GAME.GUILD_ID.eq(guildId))
                .fetchOptional() // SQL AVG may return null for empty sets
                .map(Record1::component1)
                .orElse(BigDecimal.ZERO)
        ));
    }

    @CheckReturnValue
    public CompletionStage<Set<Integer>> getDistinctPlayerSizes() {
        Summary.Child timer = MetricsRegistry.queryTime.labels("getDistinctPlayerSizes");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .selectDistinct(STATS_GAME.PLAYER_SIZE)
                .from(STATS_GAME)
                .fetch()
                .intoSet(STATS_GAME.PLAYER_SIZE)
        ));
    }

    @CheckReturnValue
    public CompletionStage<Set<Integer>> getDistinctPlayerSizesInGuild(long guildId) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("getDistinctPlayerSizesInGuild");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .selectDistinct(STATS_GAME.PLAYER_SIZE)
                .from(STATS_GAME)
                .where(STATS_GAME.GUILD_ID.eq(guildId))
                .fetch()
                .intoSet(STATS_GAME.PLAYER_SIZE)
        ));
    }

    @CheckReturnValue
    public CompletionStage<Integer> countAlignmentWins(Alignments alignment) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("countAlignmentWins");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .select(count())
                .from(STATS_GAME)
                .innerJoin(STATS_TEAM)
                .on(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID))
                .where(STATS_TEAM.IS_WINNER.isTrue())
                .and(STATS_TEAM.ALIGNMENT.eq(alignment.name()))
                .fetchOne()
                .component1()
        ));
    }

    @CheckReturnValue
    public CompletionStage<Integer> countAlignmentWinsInGuild(Alignments alignment, long guildId) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("countAlignmentWinsInGuild");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .select(count())
                .from(STATS_GAME)
                .innerJoin(STATS_TEAM)
                .on(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID))
                .where(STATS_TEAM.IS_WINNER.isTrue())
                .and(STATS_TEAM.ALIGNMENT.eq(alignment.name()))
                .and(STATS_GAME.GUILD_ID.eq(guildId))
                .fetchOne()
                .component1()
        ));
    }

    @CheckReturnValue
    public CompletionStage<Integer> countAlignmentWinsForPlayerSize(Alignments alignment, int playerSize) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("countAlignmentWinsForPlayerSize");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .select(count())
                .from(STATS_GAME)
                .innerJoin(STATS_TEAM)
                .on(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID))
                .where(STATS_TEAM.IS_WINNER.isTrue())
                .and(STATS_GAME.PLAYER_SIZE.eq(playerSize))
                .and(STATS_TEAM.ALIGNMENT.eq(alignment.name()))
                .fetchOne()
                .component1()
        ));
    }

    @CheckReturnValue
    public CompletionStage<Integer> countAlignmentWinsForPlayerSizeInGuild(Alignments alignment, int playerSize, long guildId) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("countAlignmentWinsForPlayerSizeInGuild");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .select(count())
                .from(STATS_GAME)
                .innerJoin(STATS_TEAM)
                .on(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID))
                .where(STATS_TEAM.IS_WINNER.isTrue())
                .and(STATS_GAME.PLAYER_SIZE.eq(playerSize))
                .and(STATS_TEAM.ALIGNMENT.eq(alignment.name()))
                .and(STATS_GAME.GUILD_ID.eq(guildId))
                .fetchOne()
                .component1()
        ));
    }

    @CheckReturnValue
    public CompletionStage<List<GeneralUserStats>> getGeneralUserStats(long userId) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("getGeneralUserStats");
        return this.wrapper.jooq(dsl -> timer.time(() -> dsl
                .select(STATS_PLAYER.TOTAL_POSTLENGTH, STATS_PLAYER.TOTAL_POSTS, STATS_PLAYER.ALIGNMENT, STATS_TEAM.IS_WINNER)
                .from(STATS_PLAYER)
                .innerJoin(STATS_TEAM).on(STATS_PLAYER.TEAM_ID.eq(STATS_TEAM.TEAM_ID))
                .where(STATS_PLAYER.USER_ID.eq(userId))
                .fetchInto(ImmutableGeneralUserStats.class)
        ));
    }

    @CheckReturnValue
    public CompletionStage<List<String>> getUserShots(long userId) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("getUserShots");
        return this.wrapper.jooq(dsl -> timer.time(() -> Arrays.asList(dsl
                .select(STATS_PLAYER.ALIGNMENT)
                .from(STATS_ACTION)
                .innerJoin(STATS_PLAYER).on(STATS_PLAYER.USER_ID.eq(STATS_ACTION.TARGET))
                .innerJoin(STATS_TEAM).on(STATS_TEAM.TEAM_ID.eq(STATS_PLAYER.TEAM_ID))
                .innerJoin(STATS_GAME).on(STATS_ACTION.GAME_ID.eq(STATS_GAME.GAME_ID).and(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID)))
                .where(STATS_ACTION.ACTION_TYPE.eq("SHOOT").and(STATS_ACTION.ACTOR.eq(userId)))
                .fetch()
                .intoArray(STATS_PLAYER.ALIGNMENT)
        )));
    }

    @CheckReturnValue
    public CompletionStage<Optional<GameStats>> findGameStats(long gameId) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("findGameStats");
        return this.wrapper.jooq(dsl -> timer.time(() -> {
                    Optional<GameStats> gameOpt = dsl.selectFrom(STATS_GAME)
                            .where(STATS_GAME.GAME_ID.eq(gameId))
                            .fetchOptionalInto(GameStats.class);

                    if (gameOpt.isEmpty()) {
                        return gameOpt;
                    }

                    GameStats game = gameOpt.get();

                    List<TeamStats> teams = dsl.selectFrom(STATS_TEAM)
                            .where(STATS_TEAM.GAME_ID.eq(gameId))
                            .fetch(teamMapper(game));
                    game.setTeams(teams);

                    for (TeamStats teamStats : teams) {
                        List<PlayerStats> players = dsl.selectFrom(STATS_PLAYER)
                                .where(STATS_PLAYER.TEAM_ID.eq(teamStats.getTeamId().orElseThrow()))
                                .fetch(playerMapper(teamStats));
                        teamStats.setPlayers(players);
                    }

                    List<ActionStats> actions = dsl.selectFrom(STATS_ACTION)
                            .where(STATS_ACTION.GAME_ID.eq(gameId))
                            .fetch(actionMapper(game));
                    game.setActions(actions);

                    return Optional.of(game);
                }
        ));
    }

    @CheckReturnValue
    public CompletionStage<GameStats> insertGameStats(GameStats gameStats) {
        Summary.Child timer = MetricsRegistry.queryTime.labels("insertGameStats");
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> timer.time(() -> {
                    DSLContext context = DSL.using(config);

                    long gameId = context
                            .insertInto(STATS_GAME)
                            .values(defaultValue(STATS_GAME.GAME_ID), gameStats.getChannelId(),
                                    gameStats.getChannelName(), gameStats.getEndTime(), gameStats.getGameMode().name(),
                                    gameStats.getGameType().name(), gameStats.getGuildId(), gameStats.getGuildName(),
                                    gameStats.getStartTime(), gameStats.getPlayerSize())
                            .returningResult(STATS_GAME.GAME_ID)
                            .fetchOne()
                            .component1();
                    gameStats.setGameId(gameId);

                    for (TeamStats teamStats : gameStats.getStartingTeams()) {
                        long teamId = context
                                .insertInto(STATS_TEAM)
                                .values(defaultValue(STATS_TEAM.TEAM_ID), teamStats.getAlignment(),
                                        teamStats.isWinner(), teamStats.getName(), gameId, teamStats.getTeamSize())
                                .returningResult(STATS_TEAM.TEAM_ID)
                                .fetchOne()
                                .component1();
                        teamStats.setTeamId(teamId);


                        for (PlayerStats playerStats : teamStats.getPlayers()) {
                            long playerId = context
                                    .insertInto(STATS_PLAYER)
                                    .values(defaultValue(STATS_PLAYER.PLAYER_ID), playerStats.getNickname(),
                                            playerStats.getRole().name(), playerStats.getTotalPostLength(),
                                            playerStats.getTotalPosts(), playerStats.getUserId(), teamId,
                                            playerStats.getAlignment().name())
                                    .returningResult(STATS_PLAYER.PLAYER_ID)
                                    .fetchOne()
                                    .component1();
                            playerStats.setPlayerId(playerId);
                        }
                    }

                    for (ActionStats actionStats : gameStats.getActions()) {
                        long actionId = context
                                .insertInto(STATS_ACTION)
                                .values(defaultValue(STATS_ACTION.ACTION_ID), actionStats.getActionType().name(),
                                        actionStats.getActor(), actionStats.getCycle(), actionStats.getOrder(),
                                        actionStats.getTarget(), actionStats.getTimeStampHappened(),
                                        actionStats.getTimeStampSubmitted(), gameId, actionStats.getPhase().name(),
                                        actionStats.getAdditionalInfo())
                                .returningResult(STATS_ACTION.ACTION_ID)
                                .fetchOne()
                                .component1();
                        actionStats.setActionId(actionId);
                    }

                    return gameStats;
                }
        )));
    }

    private RecordMapper<StatsTeamRecord, TeamStats> teamMapper(GameStats gameStats) {
        return record -> new TeamStats(record.getTeamId(), record.getAlignment(), record.getIsWinner(),
                record.getName(), gameStats, record.getTeamSize());
    }

    private RecordMapper<StatsPlayerRecord, PlayerStats> playerMapper(TeamStats teamStats) {
        return record -> new PlayerStats(record.getPlayerId(), record.getNickname(), record.getRole(),
                record.getTotalPostlength(), record.getTotalPosts(), record.getUserId(), teamStats, record.getAlignment());
    }

    private RecordMapper<StatsActionRecord, ActionStats> actionMapper(GameStats gameStats) {
        return record -> new ActionStats(record.getActionId(), record.getActionType(), record.getActor(),
                record.getCycle(), record.getSequence(), record.getTarget(), record.getHappened(),
                record.getSubmitted(), gameStats, record.getPhase(), record.getAdditionalInfo());
    }
}
