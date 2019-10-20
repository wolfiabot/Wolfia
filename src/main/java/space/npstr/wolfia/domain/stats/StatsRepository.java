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

import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.game.definitions.Alignments;

import javax.annotation.CheckReturnValue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.jooq.impl.DSL.avg;
import static org.jooq.impl.DSL.count;
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
        return this.wrapper.jooq(dsl -> dsl
                .select(avg(STATS_GAME.PLAYER_SIZE))
                .from(STATS_GAME)
                .fetchOne()
                .component1()
        );
    }

    @CheckReturnValue
    public CompletionStage<BigDecimal> getAveragePlayerSizeInGuild(long guildId) {
        return this.wrapper.jooq(dsl -> dsl
                .select(avg(STATS_GAME.PLAYER_SIZE))
                .from(STATS_GAME)
                .where(STATS_GAME.GUILD_ID.eq(guildId))
                .fetchOne()
                .component1()
        );
    }

    @CheckReturnValue
    public CompletionStage<Set<Integer>> getDistinctPlayerSizes() {
        return this.wrapper.jooq(dsl -> dsl
                .selectDistinct(STATS_GAME.PLAYER_SIZE)
                .from(STATS_GAME)
                .fetch()
                .intoSet(STATS_GAME.PLAYER_SIZE)
        );
    }

    @CheckReturnValue
    public CompletionStage<Set<Integer>> getDistinctPlayerSizesInGuild(long guildId) {
        return this.wrapper.jooq(dsl -> dsl
                .selectDistinct(STATS_GAME.PLAYER_SIZE)
                .from(STATS_GAME)
                .where(STATS_GAME.GUILD_ID.eq(guildId))
                .fetch()
                .intoSet(STATS_GAME.PLAYER_SIZE)
        );
    }

    @CheckReturnValue
    CompletionStage<Integer> countAlignmentWins(Alignments alignment) {
        return this.wrapper.jooq(dsl -> dsl
                .select(count())
                .from(STATS_GAME)
                .innerJoin(STATS_TEAM)
                .on(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID))
                .where(STATS_TEAM.IS_WINNER.isTrue())
                .and(STATS_TEAM.ALIGNMENT.eq(alignment.name()))
                .fetchOne()
                .component1()
        );
    }

    @CheckReturnValue
    CompletionStage<Integer> countAlignmentWinsInGuild(Alignments alignment, long guildId) {
        return this.wrapper.jooq(dsl -> dsl
                .select(count())
                .from(STATS_GAME)
                .innerJoin(STATS_TEAM)
                .on(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID))
                .where(STATS_TEAM.IS_WINNER.isTrue())
                .and(STATS_TEAM.ALIGNMENT.eq(alignment.name()))
                .and(STATS_GAME.GUILD_ID.eq(guildId))
                .fetchOne()
                .component1()
        );
    }

    @CheckReturnValue
    CompletionStage<Integer> countAlignmentWinsForPlayerSize(Alignments alignment, int playerSize) {
        return this.wrapper.jooq(dsl -> dsl
                .select(count())
                .from(STATS_GAME)
                .innerJoin(STATS_TEAM)
                .on(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID))
                .where(STATS_TEAM.IS_WINNER.isTrue())
                .and(STATS_GAME.PLAYER_SIZE.eq(playerSize))
                .and(STATS_TEAM.ALIGNMENT.eq(alignment.name()))
                .fetchOne()
                .component1()
        );
    }

    @CheckReturnValue
    CompletionStage<Integer> countAlignmentWinsForPlayerSizeInGuild(Alignments alignment, int playerSize, long guildId) {
        return this.wrapper.jooq(dsl -> dsl
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
        );
    }

    @CheckReturnValue
    CompletionStage<List<GeneralUserStats>> getGeneralUserStats(long userId) {
        return this.wrapper.jooq(dsl -> dsl
                .select(STATS_PLAYER.TOTAL_POSTLENGTH, STATS_PLAYER.TOTAL_POSTS, STATS_PLAYER.ALIGNMENT, STATS_TEAM.IS_WINNER)
                .from(STATS_PLAYER)
                .innerJoin(STATS_TEAM).on(STATS_PLAYER.TEAM_ID.eq(STATS_TEAM.TEAM_ID))
                .where(STATS_PLAYER.USER_ID.eq(userId))
                .fetchInto(ImmutableGeneralUserStats.class)
        );
    }

    @CheckReturnValue
    CompletionStage<List<String>> getUserShots(long userId) {
        return this.wrapper.jooq(dsl -> Arrays.asList(dsl
                .select(STATS_PLAYER.ALIGNMENT)
                .from(STATS_ACTION)
                .innerJoin(STATS_PLAYER).on(STATS_PLAYER.USER_ID.eq(STATS_ACTION.TARGET))
                .innerJoin(STATS_TEAM).on(STATS_TEAM.TEAM_ID.eq(STATS_PLAYER.TEAM_ID))
                .innerJoin(STATS_GAME).on(STATS_ACTION.GAME_ID.eq(STATS_GAME.GAME_ID).and(STATS_TEAM.GAME_ID.eq(STATS_GAME.GAME_ID)))
                .where(STATS_ACTION.ACTION_TYPE.eq("SHOOT").and(STATS_ACTION.ACTOR.eq(userId)))
                .fetch()
                .intoArray(STATS_PLAYER.ALIGNMENT)
        ));
    }
}
