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

package space.npstr.wolfia.domain.staff;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.gen.enums.StaffFunction;
import space.npstr.wolfia.db.gen.tables.records.StaffMemberRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;
import static space.npstr.wolfia.db.gen.Tables.STAFF_MEMBER;

class StaffRepositoryTest extends ApplicationTest {

    @Autowired
    private StaffRepository repo;

    @Autowired
    private AsyncDbWrapper wrapper;


    @Test
    void givenNoRecord_whenGetStaffMember_returnEmpty() {
        long userId = uniqueLong();

        Optional<StaffMemberRecord> staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join();

        assertThat(staffMember).isEmpty();
    }

    @Test
    void givenExistingRecord_whenGetStaffMember_returnExistingRecord() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        Optional<StaffMemberRecord> staffMemberRecord = repo.getStaffMember(userId)
                .toCompletableFuture().join();

        assertThat(staffMemberRecord).hasValueSatisfying(
                staffMember -> assertThat(staffMember.getUserId()).isEqualTo(userId)
        );
    }

    @Test
    void givenNoRecords_whenFetchAllStaffMembers_shouldBeEmpty() {
        wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .deleteFrom(STAFF_MEMBER)
                .execute()
        )).toCompletableFuture().join();

        List<StaffMemberRecord> staffMemberRecords = repo.fetchAllStaffMembers()
                .toCompletableFuture().join();

        assertThat(staffMemberRecords).isEmpty();
    }


    @Test
    void givenSomeRecords_whenFetchAllStaffMembers_shoulContainRecords() {
        wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .deleteFrom(STAFF_MEMBER)
                .execute()
        )).toCompletableFuture().join();

        long userIdA = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userIdA, StaffFunction.SETUP_MANAGER)
                .toCompletableFuture().join();

        long userIdB = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userIdB, StaffFunction.MODERATOR)
                .toCompletableFuture().join();


        List<StaffMemberRecord> staffMemberRecords = repo.fetchAllStaffMembers()
                .toCompletableFuture().join();

        assertThat(staffMemberRecords).hasSize(2);
        assertThat(staffMemberRecords).anySatisfy(staffMemberRecord -> {
            assertThat(staffMemberRecord.getUserId()).isEqualTo(userIdA);
            assertThat(staffMemberRecord.getFunction()).isEqualTo(StaffFunction.SETUP_MANAGER);
        });
        assertThat(staffMemberRecords).anySatisfy(staffMemberRecord -> {
            assertThat(staffMemberRecord.getUserId()).isEqualTo(userIdB);
            assertThat(staffMemberRecord.getFunction()).isEqualTo(StaffFunction.MODERATOR);
        });
    }


    @Test
    void whenUpdateStaffMemberFunction_shouldReturnRecord() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getFunction()).isEqualTo(StaffFunction.DEVELOPER);
    }

    @Test
    void givenNoRecord_whenUpdateStaffMemberFunction_shouldCreateNewStaffMember() {
        long userId = uniqueLong();

        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        StaffMemberRecord staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getFunction()).isEqualTo(StaffFunction.DEVELOPER);
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberFunction_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();
        StaffMemberRecord staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getUserId()).isEqualTo(userId);


        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.MODERATOR)
                .toCompletableFuture().join();
        staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getFunction()).isEqualTo(StaffFunction.MODERATOR);
    }


    @Test
    void defaultStaffMemberSlogan_shouldBeNull() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        assertThat(staffMember.getSlogan()).isNull();
    }

    @Test
    void givenNoExistingRecord_whenUpdateStaffMemberSlogan_shouldReturnEmpty() {
        long userId = uniqueLong();

        Optional<StaffMemberRecord> staffMember = repo.updateSlogan(userId, "foo")
                .toCompletableFuture().join();

        assertThat(staffMember).isEmpty();
    }

    @Test
    void whenUpdateStaffMemberSlogan_shouldReturnRecord() {
        long userId = uniqueLong();
        String slogan = "foo";
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        StaffMemberRecord staffMember = repo.updateSlogan(userId, slogan)
                .toCompletableFuture().join().orElseThrow();

        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getSlogan()).isEqualTo(slogan);
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberSlogan_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        String slogan = "foo";
        repo.updateSlogan(userId, slogan)
                .toCompletableFuture().join();
        StaffMemberRecord staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getSlogan()).isEqualTo(slogan);
    }

    @Test
    void whenUpdateStaffMemberSloganWithNull_shouldRemoveSlogan() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        repo.updateSlogan(userId, null)
                .toCompletableFuture().join();
        StaffMemberRecord staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getSlogan()).isNull();
    }


    @Test
    void defaultStaffMemberLink_shouldBeNull() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        assertThat(staffMember.getLink()).isNull();
    }

    @Test
    void givenNoExistingRecord_whenUpdateStaffMemberLink_shouldReturnEmpty() {
        long userId = uniqueLong();

        Optional<StaffMemberRecord> staffMember = repo.updateLink(userId, URI.create("https://example.org"))
                .toCompletableFuture().join();

        assertThat(staffMember).isEmpty();
    }

    @Test
    void whenUpdateStaffMemberLink_shouldReturnRecord() {
        long userId = uniqueLong();
        URI link = URI.create("https://example.org");
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        StaffMemberRecord staffMember = repo.updateLink(userId, link)
                .toCompletableFuture().join().orElseThrow();

        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getLink()).isEqualTo(link);
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberLink_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        URI link = URI.create("https://example.org");
        repo.updateLink(userId, link)
                .toCompletableFuture().join();
        StaffMemberRecord staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getLink()).isEqualTo(link);
    }

    @Test
    void whenUpdateStaffMemberLinkWithNull_shouldRemoveLink() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        repo.updateLink(userId, null)
                .toCompletableFuture().join();
        StaffMemberRecord staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getLink()).isNull();
    }


    @Test
    void defaultStaffMemberEnabled_shouldBeFalse() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        assertThat(staffMember.getEnabled()).isFalse();
    }

    @Test
    void givenNoExistingRecord_whenUpdateStaffMemberEnabled_shouldReturnEmpty() {
        long userId = uniqueLong();

        Optional<StaffMemberRecord> staffMember = repo.updateEnabled(userId, true)
                .toCompletableFuture().join();

        assertThat(staffMember).isEmpty();
    }

    @Test
    void whenUpdateStaffMemberEnabled_shouldReturnRecord() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        StaffMemberRecord staffMember = repo.updateEnabled(userId, true)
                .toCompletableFuture().join().orElseThrow();

        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getEnabled()).isTrue();
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberEnabled_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER)
                .toCompletableFuture().join();

        repo.updateEnabled(userId, true)
                .toCompletableFuture().join();
        StaffMemberRecord staffMember = repo.getStaffMember(userId)
                .toCompletableFuture().join()
                .orElseThrow();
        assertThat(staffMember.getEnabled()).isTrue();
    }


    @Test
    void whenSetActive_passedIdsShouldBeActive() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER).toCompletableFuture().join();

        repo.updateAllActive(List.of(userA, userB)).toCompletableFuture().join();

        StaffMemberRecord staffA = repo.getStaffMember(userA).toCompletableFuture().join().orElseThrow();
        assertThat(staffA.getActive()).isTrue();
        StaffMemberRecord staffB = repo.getStaffMember(userB).toCompletableFuture().join().orElseThrow();
        assertThat(staffB.getActive()).isTrue();
    }

    @Test
    void whenSetActive_notPassedIdsShouldNotBeActive() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER).toCompletableFuture().join();

        repo.updateAllActive(List.of(userA, userB)).toCompletableFuture().join();

        StaffMemberRecord staffC = repo.getStaffMember(userC).toCompletableFuture().join().orElseThrow();
        assertThat(staffC.getActive()).isFalse();
    }

    @Test
    void whenSetActiveWithEmptySet_noneShouldBeActive() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER).toCompletableFuture().join();

        repo.updateAllActive(Collections.emptyList()).toCompletableFuture().join();

        StaffMemberRecord staffA = repo.getStaffMember(userA).toCompletableFuture().join().orElseThrow();
        assertThat(staffA.getActive()).isFalse();
        StaffMemberRecord staffB = repo.getStaffMember(userB).toCompletableFuture().join().orElseThrow();
        assertThat(staffB.getActive()).isFalse();
        StaffMemberRecord staffC = repo.getStaffMember(userC).toCompletableFuture().join().orElseThrow();
        assertThat(staffC.getActive()).isFalse();
    }

    @Test
    void whenSetActiveWithIdsOfNonExistentStaff_shouldBeIgnored() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER).toCompletableFuture().join();
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER).toCompletableFuture().join();

        repo.updateAllActive(List.of(userB, userC, uniqueLong(), uniqueLong())).toCompletableFuture().join();

        StaffMemberRecord staffA = repo.getStaffMember(userA).toCompletableFuture().join().orElseThrow();
        assertThat(staffA.getActive()).isFalse();
        StaffMemberRecord staffB = repo.getStaffMember(userB).toCompletableFuture().join().orElseThrow();
        assertThat(staffB.getActive()).isTrue();
        StaffMemberRecord staffC = repo.getStaffMember(userC).toCompletableFuture().join().orElseThrow();
        assertThat(staffC.getActive()).isTrue();
    }
}
