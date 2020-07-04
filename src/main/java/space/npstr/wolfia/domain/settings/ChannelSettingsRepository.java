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

package space.npstr.wolfia.domain.settings;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.CheckReturnValue;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;

import static space.npstr.wolfia.db.ExtendedPostgresDSL.arrayAppendDistinct;
import static space.npstr.wolfia.db.ExtendedPostgresDSL.arrayDiff;
import static space.npstr.wolfia.db.gen.Tables.CHANNEL_SETTINGS;

/**
 * Saves settings on a per channel basis. The difference to {@link space.npstr.wolfia.db.gen.tables.records.GameSetupRecord}
 * is that this record contains technical Discord stuff, while {@link space.npstr.wolfia.db.gen.tables.records.GameSetupRecord}
 * should contain purely game related stuff.
 */
@Repository
public class ChannelSettingsRepository {

    private final AsyncDbWrapper wrapper;

    public ChannelSettingsRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<Optional<ChannelSettings>> findOne(long channelId) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(CHANNEL_SETTINGS)
                .where(CHANNEL_SETTINGS.CHANNEL_ID.eq(channelId))
                .fetchOptionalInto(ChannelSettings.class)
        );
    }

    @CheckReturnValue
    //this works since we dont commit the transaction
    public CompletionStage<ChannelSettings> findOneOrDefault(long channelId) {
        return this.wrapper.jooq(dsl -> dsl
                .insertInto(CHANNEL_SETTINGS)
                .columns(CHANNEL_SETTINGS.CHANNEL_ID)
                .values(channelId)
                .onDuplicateKeyUpdate() // cant ignore, otherwise returning() will be empty on conflict
                .set(CHANNEL_SETTINGS.CHANNEL_ID, channelId)
                .returning()
                .fetchOne()
                .into(ChannelSettings.class)
        );
    }

    @CheckReturnValue
    public CompletionStage<List<ChannelSettings>> findOrDefault(Collection<Long> channelIds) {
        if (channelIds.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return this.wrapper.jooq(dsl -> {
            var insert = dsl.insertInto(CHANNEL_SETTINGS)
                    .columns(CHANNEL_SETTINGS.CHANNEL_ID);
            for (long channelId : channelIds) {
                insert = insert.values(channelId);
            }
            // See https://github.com/jOOQ/jOOQ/issues/5214#issuecomment-213574749
            var excluded = CHANNEL_SETTINGS.as("excluded");
            return insert
                    .onDuplicateKeyUpdate() // cant ignore, otherwise returning() will be empty on conflict
                    .set(CHANNEL_SETTINGS.CHANNEL_ID, excluded.CHANNEL_ID)
                    .returning()
                    .fetch()
                    .into(ChannelSettings.class);
        });
    }

    @CheckReturnValue
    public CompletionStage<ChannelSettings> setAccessRoleId(long channelId, long accessRoleId) {
        return set(channelId, CHANNEL_SETTINGS.ACCESS_ROLE_ID, accessRoleId);
    }

    @CheckReturnValue
    public CompletionStage<ChannelSettings> setAutoOut(long channelId, boolean autoOut) {
        return set(channelId, CHANNEL_SETTINGS.AUTO_OUT, autoOut);
    }

    @CheckReturnValue
    public CompletionStage<ChannelSettings> setGameChannel(long channelId, boolean isGameChannel) {
        return set(channelId, CHANNEL_SETTINGS.IS_GAME_CHANNEL, isGameChannel);
    }

    @CheckReturnValue
    public CompletionStage<ChannelSettings> setTagCooldown(long channelId, long tagCooldown) {
        return set(channelId, CHANNEL_SETTINGS.TAG_COOLDOWN, tagCooldown);
    }

    @CheckReturnValue
    public CompletionStage<ChannelSettings> setTagLastUsed(long channelId, long lastUsed) {
        return set(channelId, CHANNEL_SETTINGS.TAG_LAST_USED, lastUsed);
    }

    @CheckReturnValue
    public CompletionStage<ChannelSettings> addTags(long channelId, Collection<Long> tags) {
        Long[] tagArray = tags.toArray(new Long[0]);
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(CHANNEL_SETTINGS)
                .columns(CHANNEL_SETTINGS.CHANNEL_ID, CHANNEL_SETTINGS.TAGS)
                .values(channelId, tagArray)
                .onDuplicateKeyUpdate()
                .set(CHANNEL_SETTINGS.TAGS, arrayAppendDistinct(CHANNEL_SETTINGS.TAGS, tagArray))
                .returning()
                .fetchOne()
                .into(ChannelSettings.class)
        ));
    }

    @CheckReturnValue
    public CompletionStage<ChannelSettings> removeTags(long channelId, Collection<Long> tags) {
        Long[] tagArray = tags.toArray(new Long[0]);
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(CHANNEL_SETTINGS)
                .columns(CHANNEL_SETTINGS.CHANNEL_ID, CHANNEL_SETTINGS.TAGS)
                .values(channelId, new Long[]{})
                .onDuplicateKeyUpdate()
                .set(CHANNEL_SETTINGS.TAGS, arrayDiff(CHANNEL_SETTINGS.TAGS, tagArray))
                .returning()
                .fetchOne()
                .into(ChannelSettings.class)
        ));
    }

    @CheckReturnValue
    public CompletionStage<Integer> delete(long channelId) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .deleteFrom(CHANNEL_SETTINGS)
                .where(CHANNEL_SETTINGS.CHANNEL_ID.eq(channelId))
                .execute()
        ));
    }

    @CheckReturnValue
    private <F> CompletionStage<ChannelSettings> set(long channelId, Field<F> field, F value) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(CHANNEL_SETTINGS)
                .columns(CHANNEL_SETTINGS.CHANNEL_ID, field)
                .values(channelId, value)
                .onDuplicateKeyUpdate()
                .set(field, value)
                .returning()
                .fetchOne()
                .into(ChannelSettings.class)
        ));
    }

}
