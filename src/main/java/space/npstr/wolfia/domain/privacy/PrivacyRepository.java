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

package space.npstr.wolfia.domain.privacy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.CheckReturnValue;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;

import static space.npstr.wolfia.db.gen.Tables.DISCORD_USER;

@Repository
public class PrivacyRepository {

    private final AsyncDbWrapper wrapper;

    public PrivacyRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<Optional<Privacy>> findOne(long userId) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(DISCORD_USER)
                .where(DISCORD_USER.USER_ID.eq(userId))
                .fetchOptionalInto(Privacy.class));
    }

    @CheckReturnValue
    public CompletionStage<Privacy> setProcessData(long userId, boolean processData) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(DISCORD_USER)
                .columns(DISCORD_USER.USER_ID, DISCORD_USER.PROCESS_DATA)
                .values(userId, processData)
                .onDuplicateKeyUpdate()
                .set(DISCORD_USER.PROCESS_DATA, processData)
                .returning()
                .fetchOne()
                .into(Privacy.class)
        ));
    }

    @CheckReturnValue
    public CompletionStage<List<Privacy>> findAllDeniedProcessData() {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(DISCORD_USER)
                .where(DISCORD_USER.PROCESS_DATA.eq(false))
                .fetchInto(Privacy.class)
        );
    }

}
