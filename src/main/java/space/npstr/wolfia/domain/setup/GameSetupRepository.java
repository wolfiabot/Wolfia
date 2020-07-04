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

package space.npstr.wolfia.domain.setup;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

import javax.annotation.CheckReturnValue;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static space.npstr.wolfia.db.ExtendedPostgresDSL.arrayAppendDistinct;
import static space.npstr.wolfia.db.ExtendedPostgresDSL.arrayDiff;
import static space.npstr.wolfia.db.gen.Tables.CHANNEL_SETTINGS;
import static space.npstr.wolfia.db.gen.Tables.GAME_SETUP;


@Component
public class GameSetupRepository {

    private final AsyncDbWrapper wrapper;

    public GameSetupRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<Optional<GameSetup>> findOne(long channelId) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(GAME_SETUP)
                .where(GAME_SETUP.CHANNEL_ID.eq(channelId))
                .fetchOptionalInto(GameSetup.class)
        );
    }

    @CheckReturnValue
    //this works since we dont commit the transaction
    public CompletionStage<GameSetup> findOneOrDefault(long channelId) {
        return this.wrapper.jooq(dsl -> dsl
                .insertInto(GAME_SETUP)
                .columns(GAME_SETUP.CHANNEL_ID)
                .values(channelId)
                .onDuplicateKeyUpdate() // cant ignore, otherwise returning() will be empty on conflict
                .set(GAME_SETUP.CHANNEL_ID, channelId)
                .returning()
                .fetchOne()
                .into(GameSetup.class)
        );
    }

    @CheckReturnValue
    public CompletionStage<List<GameSetup>> findAutoOutSetupsWhereUserIsInned(long userId) {
        return this.wrapper.jooq(dsl -> dsl
                .select(GAME_SETUP.CHANNEL_ID, GAME_SETUP.INNED_USERS, GAME_SETUP.GAME, GAME_SETUP.MODE, GAME_SETUP.DAY_LENGTH)
                .from(GAME_SETUP)
                .join(CHANNEL_SETTINGS).on(GAME_SETUP.CHANNEL_ID.eq(CHANNEL_SETTINGS.CHANNEL_ID))
                .where(CHANNEL_SETTINGS.AUTO_OUT.isTrue())
                .and(GAME_SETUP.INNED_USERS.contains(new Long[]{userId}))
                .fetchInto(GameSetup.class)
        );
    }

    @CheckReturnValue
    public CompletionStage<GameSetup> setGame(long channelId, Games game) {
        return set(channelId, GAME_SETUP.GAME, game.name());
    }

    @CheckReturnValue
    public CompletionStage<GameSetup> setMode(long channelId, GameInfo.GameMode mode) {
        return set(channelId, GAME_SETUP.MODE, mode.name());
    }

    @CheckReturnValue
    public CompletionStage<GameSetup> setDayLength(long channelId, Duration dayLength) {
        return set(channelId, GAME_SETUP.DAY_LENGTH, dayLength.toMillis());
    }

    @CheckReturnValue
    public CompletionStage<GameSetup> inUsers(long channelId, Collection<Long> userIds) {
        Long[] userIdsArray = userIds.toArray(new Long[0]);
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(GAME_SETUP)
                .columns(GAME_SETUP.CHANNEL_ID, GAME_SETUP.INNED_USERS)
                .values(channelId, userIdsArray)
                .onDuplicateKeyUpdate()
                .set(GAME_SETUP.INNED_USERS, arrayAppendDistinct(GAME_SETUP.INNED_USERS, userIdsArray))
                .returning()
                .fetchOne()
                .into(GameSetup.class)
        ));
    }

    @CheckReturnValue
    public CompletionStage<GameSetup> outUsers(long channelId, Collection<Long> userIds) {
        Long[] userIdsArray = userIds.toArray(new Long[0]);
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(GAME_SETUP)
                .columns(GAME_SETUP.CHANNEL_ID, GAME_SETUP.INNED_USERS)
                .values(channelId, new Long[]{})
                .onDuplicateKeyUpdate()
                .set(GAME_SETUP.INNED_USERS, arrayDiff(GAME_SETUP.INNED_USERS, userIdsArray))
                .returning()
                .fetchOne()
                .into(GameSetup.class)
        ));
    }

    @CheckReturnValue
    public CompletionStage<Integer> delete(long channelId) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .deleteFrom(GAME_SETUP)
                .where(GAME_SETUP.CHANNEL_ID.eq(channelId))
                .execute()
        ));
    }

    @CheckReturnValue
    private <F> CompletionStage<GameSetup> set(long channelId, Field<F> field, F value) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(GAME_SETUP)
                .columns(GAME_SETUP.CHANNEL_ID, field)
                .values(channelId, value)
                .onDuplicateKeyUpdate()
                .set(field, value)
                .returning()
                .fetchOne()
                .into(GameSetup.class)));
    }
}
