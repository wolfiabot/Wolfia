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

package space.npstr.wolfia.domain.staff;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.CheckReturnValue;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.gen.enums.StaffFunction;
import space.npstr.wolfia.db.gen.tables.records.StaffMemberRecord;

import static org.jooq.impl.DSL.not;
import static space.npstr.wolfia.db.gen.Tables.STAFF_MEMBER;

/**
 * Persist staff member data.
 */
@Repository
public class StaffRepository {

    private final AsyncDbWrapper wrapper;

    public StaffRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<Optional<StaffMemberRecord>> getStaffMember(long userId) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(STAFF_MEMBER)
                .where(STAFF_MEMBER.USER_ID.eq(userId))
                .fetchOptional()
        );
    }

    @CheckReturnValue
    public CompletionStage<List<StaffMemberRecord>> fetchAllStaffMembers() {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(STAFF_MEMBER)
                .fetch()
        );
    }

    @CheckReturnValue
    public CompletionStage<StaffMemberRecord> updateOrCreateStaffMemberFunction(long userId, StaffFunction staffFunction) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> config.dsl()
                .insertInto(STAFF_MEMBER)
                .columns(STAFF_MEMBER.USER_ID, STAFF_MEMBER.FUNCTION)
                .values(userId, staffFunction)
                .onDuplicateKeyUpdate()
                .set(STAFF_MEMBER.FUNCTION, staffFunction)
                .returning()
                .fetchOne()
        ));
    }

    @CheckReturnValue
    public CompletionStage<Optional<StaffMemberRecord>> updateSlogan(long userId, @Nullable String slogan) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> config.dsl()
                .update(STAFF_MEMBER)
                .set(STAFF_MEMBER.SLOGAN, slogan)
                .where(STAFF_MEMBER.USER_ID.eq(userId))
                .returning()
                .fetchOptional()
        ));
    }

    @CheckReturnValue
    public CompletionStage<Optional<StaffMemberRecord>> updateLink(long userId, @Nullable URI link) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> config.dsl()
                .update(STAFF_MEMBER)
                .set(STAFF_MEMBER.LINK, link)
                .where(STAFF_MEMBER.USER_ID.eq(userId))
                .returning()
                .fetchOptional()
        ));
    }

    @CheckReturnValue
    public CompletionStage<Optional<StaffMemberRecord>> updateEnabled(long userId, boolean enabled) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> config.dsl()
                .update(STAFF_MEMBER)
                .set(STAFF_MEMBER.ENABLED, enabled)
                .where(STAFF_MEMBER.USER_ID.eq(userId))
                .returning()
                .fetchOptional()
        ));
    }

    @CheckReturnValue
    public CompletionStage<Void> updateAllActive(Collection<Long> activeStaff) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> config.dsl()
                .update(STAFF_MEMBER)
                .set(STAFF_MEMBER.ACTIVE, true)
                .where(STAFF_MEMBER.USER_ID.in(activeStaff))
                .execute()
        )).thenCompose(__ -> this.wrapper.jooq(dsl -> dsl.transactionResult(config -> config.dsl()
                .update(STAFF_MEMBER)
                .set(STAFF_MEMBER.ACTIVE, false)
                .where(not(STAFF_MEMBER.USER_ID.in(activeStaff)))
                .execute()
        ))).thenApply(__ -> null);
    }
}
