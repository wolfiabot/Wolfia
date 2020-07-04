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

package space.npstr.wolfia.domain.ban;

import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.gen.tables.records.BanRecord;
import space.npstr.wolfia.game.definitions.Scope;

import javax.annotation.CheckReturnValue;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static space.npstr.wolfia.db.gen.Tables.BAN;

@Repository
public class BanRepository {

    private final AsyncDbWrapper wrapper;

    public BanRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<Optional<BanRecord>> findOne(long userId, Scope scope) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(BAN)
                .where(BAN.USER_ID.eq(userId).and(BAN.SCOPE.eq(scope.name())))
                .fetchOptional());
    }

    @CheckReturnValue
    public CompletionStage<BanRecord> setScope(long userId, Scope scope) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(BAN)
                .columns(BAN.USER_ID, BAN.SCOPE)
                .values(userId, scope.name())
                .onDuplicateKeyUpdate()
                .set(BAN.SCOPE, scope.name())
                .returning()
                .fetchOne()
        ));
    }

    @CheckReturnValue
    public CompletionStage<List<BanRecord>> findByScope(Scope scope) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(BAN)
                .where(BAN.SCOPE.eq(scope.name()))
                .fetch()
        );
    }
}
