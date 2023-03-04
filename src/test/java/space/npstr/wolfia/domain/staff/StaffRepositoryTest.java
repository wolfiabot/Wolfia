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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.gen.enums.StaffFunction;
import space.npstr.wolfia.db.gen.tables.records.StaffMemberRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;
import static space.npstr.wolfia.db.gen.Tables.STAFF_MEMBER;

class StaffRepositoryTest extends ApplicationTest {

    @Autowired
    private StaffRepository repo;

    @Autowired
    private Database database;


    @Test
    void givenNoRecord_whenGetStaffMember_returnEmpty() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.getStaffMember(userId);

        assertThat(staffMember).isNull();
    }

    @Test
    void givenExistingRecord_whenGetStaffMember_returnExistingRecord() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        StaffMemberRecord staffMemberRecord = repo.getStaffMember(userId);

        assertThat(staffMemberRecord).isNotNull();
        assertThat(staffMemberRecord.getUserId()).isEqualTo(userId);
    }

    @Test
    void givenNoRecords_whenFetchAllStaffMembers_shouldBeEmpty() {
        database.jooq().transactionResult(config -> config.dsl()
                .deleteFrom(STAFF_MEMBER)
                .execute()
        );

        List<StaffMemberRecord> staffMemberRecords = repo.fetchAllStaffMembers();

        assertThat(staffMemberRecords).isEmpty();
    }


    @Test
    void givenSomeRecords_whenFetchAllStaffMembers_shoulContainRecords() {
        database.jooq().transactionResult(config -> config.dsl()
                .deleteFrom(STAFF_MEMBER)
                .execute()
        );

        long userIdA = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userIdA, StaffFunction.SETUP_MANAGER);

        long userIdB = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userIdB, StaffFunction.MODERATOR);


        List<StaffMemberRecord> staffMemberRecords = repo.fetchAllStaffMembers();

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

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getFunction()).isEqualTo(StaffFunction.DEVELOPER);
    }

    @Test
    void givenNoRecord_whenUpdateStaffMemberFunction_shouldCreateNewStaffMember() {
        long userId = uniqueLong();

        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        StaffMemberRecord staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getFunction()).isEqualTo(StaffFunction.DEVELOPER);
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberFunction_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);
        StaffMemberRecord staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getUserId()).isEqualTo(userId);


        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.MODERATOR);
        staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getFunction()).isEqualTo(StaffFunction.MODERATOR);
    }


    @Test
    void defaultStaffMemberSlogan_shouldBeNull() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        assertThat(staffMember.getSlogan()).isNull();
    }

    @Test
    void givenNoExistingRecord_whenUpdateStaffMemberSlogan_shouldReturnEmpty() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateSlogan(userId, "foo");

        assertThat(staffMember).isNull();
    }

    @Test
    void whenUpdateStaffMemberSlogan_shouldReturnRecord() {
        long userId = uniqueLong();
        String slogan = "foo";
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        StaffMemberRecord staffMember = repo.updateSlogan(userId, slogan);

        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getSlogan()).isEqualTo(slogan);
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberSlogan_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        String slogan = "foo";
        repo.updateSlogan(userId, slogan);
        StaffMemberRecord staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getSlogan()).isEqualTo(slogan);
    }

    @Test
    void whenUpdateStaffMemberSloganWithNull_shouldRemoveSlogan() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        repo.updateSlogan(userId, null);
        StaffMemberRecord staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getSlogan()).isNull();
    }


    @Test
    void defaultStaffMemberLink_shouldBeNull() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        assertThat(staffMember.getLink()).isNull();
    }

    @Test
    void givenNoExistingRecord_whenUpdateStaffMemberLink_shouldReturnEmpty() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateLink(userId, URI.create("https://example.org"));

        assertThat(staffMember).isNull();
    }

    @Test
    void whenUpdateStaffMemberLink_shouldReturnRecord() {
        long userId = uniqueLong();
        URI link = URI.create("https://example.org");
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        StaffMemberRecord staffMember = repo.updateLink(userId, link);

        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getLink()).isEqualTo(link);
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberLink_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        URI link = URI.create("https://example.org");
        repo.updateLink(userId, link);
        StaffMemberRecord staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getLink()).isEqualTo(link);
    }

    @Test
    void whenUpdateStaffMemberLinkWithNull_shouldRemoveLink() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        repo.updateLink(userId, null);
        StaffMemberRecord staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getLink()).isNull();
    }


    @Test
    void defaultStaffMemberEnabled_shouldBeFalse() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        assertThat(staffMember.getEnabled()).isFalse();
    }

    @Test
    void givenNoExistingRecord_whenUpdateStaffMemberEnabled_shouldReturnEmpty() {
        long userId = uniqueLong();

        StaffMemberRecord staffMember = repo.updateEnabled(userId, true);

        assertThat(staffMember).isNull();
    }

    @Test
    void whenUpdateStaffMemberEnabled_shouldReturnRecord() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        StaffMemberRecord staffMember = repo.updateEnabled(userId, true);

        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getUserId()).isEqualTo(userId);
        assertThat(staffMember.getEnabled()).isTrue();
    }

    @Test
    void givenExistingRecord_whenUpdateStaffMemberEnabled_shouldUpdateStaffMember() {
        long userId = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userId, StaffFunction.DEVELOPER);

        repo.updateEnabled(userId, true);
        StaffMemberRecord staffMember = repo.getStaffMember(userId);
        assertThat(staffMember).isNotNull();
        assertThat(staffMember.getEnabled()).isTrue();
    }


    @Test
    void whenSetActive_passedIdsShouldBeActive() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR);
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER);
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER);

        repo.updateAllActive(List.of(userA, userB));

        StaffMemberRecord staffA = repo.getStaffMember(userA);
        assertThat(staffA).isNotNull();
        assertThat(staffA.getActive()).isTrue();
        StaffMemberRecord staffB = repo.getStaffMember(userB);
        assertThat(staffB).isNotNull();
        assertThat(staffB.getActive()).isTrue();
    }

    @Test
    void whenSetActive_notPassedIdsShouldNotBeActive() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR);
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER);
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER);

        repo.updateAllActive(List.of(userA, userB));

        StaffMemberRecord staffC = repo.getStaffMember(userC);
        assertThat(staffC).isNotNull();
        assertThat(staffC.getActive()).isFalse();
    }

    @Test
    void whenSetActiveWithEmptySet_noneShouldBeActive() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR);
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER);
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER);

        repo.updateAllActive(Collections.emptyList());

        StaffMemberRecord staffA = repo.getStaffMember(userA);
        assertThat(staffA).isNotNull();
        assertThat(staffA.getActive()).isFalse();
        StaffMemberRecord staffB = repo.getStaffMember(userB);
        assertThat(staffB).isNotNull();
        assertThat(staffB.getActive()).isFalse();
        StaffMemberRecord staffC = repo.getStaffMember(userC);
        assertThat(staffC).isNotNull();
        assertThat(staffC.getActive()).isFalse();
    }

    @Test
    void whenSetActiveWithIdsOfNonExistentStaff_shouldBeIgnored() {
        long userA = uniqueLong();
        long userB = uniqueLong();
        long userC = uniqueLong();
        repo.updateOrCreateStaffMemberFunction(userA, StaffFunction.MODERATOR);
        repo.updateOrCreateStaffMemberFunction(userB, StaffFunction.SETUP_MANAGER);
        repo.updateOrCreateStaffMemberFunction(userC, StaffFunction.DEVELOPER);

        repo.updateAllActive(List.of(userB, userC, uniqueLong(), uniqueLong()));

        StaffMemberRecord staffA = repo.getStaffMember(userA);
        assertThat(staffA).isNotNull();
        assertThat(staffA.getActive()).isFalse();
        StaffMemberRecord staffB = repo.getStaffMember(userB);
        assertThat(staffB).isNotNull();
        assertThat(staffB.getActive()).isTrue();
        StaffMemberRecord staffC = repo.getStaffMember(userC);
        assertThat(staffC).isNotNull();
        assertThat(staffC.getActive()).isTrue();
    }
}
