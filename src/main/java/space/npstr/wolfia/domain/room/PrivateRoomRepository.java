/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.domain.room;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.CheckReturnValue;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;

import static java.lang.Boolean.TRUE;
import static org.jooq.impl.DSL.value;
import static space.npstr.wolfia.db.gen.Tables.PRIVATE_ROOM;

@Repository
public class PrivateRoomRepository {

    private final AsyncDbWrapper wrapper;

    public PrivateRoomRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<Optional<PrivateRoom>> findOneByGuildId(long guildId) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(PRIVATE_ROOM)
                .where(PRIVATE_ROOM.GUILD_ID.eq(guildId))
                .fetchOptionalInto(PrivateRoom.class)
        );
    }

    @CheckReturnValue
    public CompletionStage<List<PrivateRoom>> findAll() {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(PRIVATE_ROOM)
                .orderBy(PRIVATE_ROOM.NR.asc())
                .fetchInto(PrivateRoom.class)
        );
    }

    // inspired by the 2. solution from https://www.eidias.com/blog/2012/1/16/finding-gaps-in-a-sequence-of-identifier-values-of
    // this can probably be written in a single query but why bother, race conditions are not expected for registering
    @CheckReturnValue
    public CompletionStage<Optional<PrivateRoom>> insert(long guildId) {
        return getFirstFreeNumber().thenCompose(firstFreeNumber ->
                this.wrapper.jooq(dsl -> dsl.transactionResult(config -> config.dsl()
                        .insertInto(PRIVATE_ROOM)
                        .columns(PRIVATE_ROOM.GUILD_ID, PRIVATE_ROOM.NR)
                        .values(guildId, firstFreeNumber)
                        .onDuplicateKeyIgnore()
                        .returning()
                        .fetchOptional()
                        .map(r -> r.into(PrivateRoom.class))
                )));
    }

    @CheckReturnValue
    private CompletionStage<Integer> getFirstFreeNumber() {
        return numberOneExists()
                .thenCompose(numberOneExists -> {
                    if (!TRUE.equals(numberOneExists)) {
                        return CompletableFuture.completedStage(1);
                    }
                    var a = PRIVATE_ROOM.as("a");
                    var b = PRIVATE_ROOM.as("b");
                    return this.wrapper.jooq(dsl -> dsl
                            .select(a.NR.add(1))
                            .from(a)
                            .whereNotExists(dsl
                                    .selectFrom(b)
                                    .where(a.NR.add(1).eq(b.NR))
                            )
                            .orderBy(a.NR.asc())
                            .limit(1)
                            .fetchOne()
                            .component1()
                    );
                });
    }

    @CheckReturnValue
    private CompletionStage<Boolean> numberOneExists() {
        return this.wrapper.jooq(dsl -> dsl
                .select(value(1))
                .from(PRIVATE_ROOM)
                .where(PRIVATE_ROOM.NR.eq(1))
                .fetchOptional()
                .isPresent()
        );
    }
}
