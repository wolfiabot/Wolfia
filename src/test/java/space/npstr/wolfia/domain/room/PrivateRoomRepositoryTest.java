/*
 * Copyright (C) 2016-2025 the original author or authors
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

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;
import static space.npstr.wolfia.db.gen.Tables.PRIVATE_ROOM;

class PrivateRoomRepositoryTest extends ApplicationTest {

    @Autowired
    private PrivateRoomRepository repository;

    @Autowired
    private DSLContext jooq;

    @BeforeEach
    @AfterEach
    void cleanDbTable() {
        this.jooq.transactionResult(config -> config.dsl()
                .deleteFrom(PRIVATE_ROOM)
                .execute()
        );
    }

    @Test
    void givenNoPrivateRooms_whenPrivateRoomInserted_createPrivateRoomWithNumber1() {
        long guildId = uniqueLong();

        PrivateRoom inserted = this.repository.insert(guildId);

        assertThat(inserted).isNotNull();
        assertThat(inserted.getGuildId()).isEqualTo(guildId);
        assertThat(inserted.getNumber()).isEqualTo(1);
    }

    @Test
    void givenExistingPrivateRoom_whenPrivateRoomWithSameGuildIdInserted_returnEmpty() {
        long guildId = preparePrivateRoom(1);

        PrivateRoom inserted = this.repository.insert(guildId);

        assertThat(inserted).isNull();
    }

    @Test
    void givenOnePrivateRoom_whenPrivateRoomInserted_createPrivateRoomWithNumber2() {
        preparePrivateRoom(1);
        long guildId = uniqueLong();

        PrivateRoom inserted = this.repository.insert(guildId);

        assertThat(inserted).isNotNull();
        assertThat(inserted.getGuildId()).isEqualTo(guildId);
        assertThat(inserted.getNumber()).isEqualTo(2);
    }

    @Test
    void givenTwoPrivateRoomsWithNumbers1And2_whenPrivateRoomInserted_createPrivateRoomWithNumber3() {
        preparePrivateRoom(1);
        preparePrivateRoom(2);
        long guildId = uniqueLong();

        PrivateRoom inserted = this.repository.insert(guildId);

        assertThat(inserted).isNotNull();
        assertThat(inserted.getGuildId()).isEqualTo(guildId);
        assertThat(inserted.getNumber()).isEqualTo(3);
    }

    @Test
    void givenTwoPrivateRoomsWithNumbers1And3_whenPrivateRoomInserted_createPrivateRoomWithNumber2() {
        preparePrivateRoom(1);
        preparePrivateRoom(3);
        long guildId = uniqueLong();

        PrivateRoom inserted = this.repository.insert(guildId);

        assertThat(inserted).isNotNull();
        assertThat(inserted.getGuildId()).isEqualTo(guildId);
        assertThat(inserted.getNumber()).isEqualTo(2);
    }

    @Test
    void givenTwoPrivateRoomsWithNumbers2And3_whenPrivateRoomInserted_createPrivateRoomWithNumber1() {
        preparePrivateRoom(2);
        preparePrivateRoom(3);
        long guildId = uniqueLong();

        PrivateRoom inserted = this.repository.insert(guildId);

        assertThat(inserted).isNotNull();
        assertThat(inserted.getGuildId()).isEqualTo(guildId);
        assertThat(inserted.getNumber()).isEqualTo(1);
    }

    @Test
    void givenABunchOfPrivateRoomsWithNumbers1To9_whenPrivateRoomInserted_createPrivateRoomWithMissingNumbers() {
        preparePrivateRoom(2);
        preparePrivateRoom(3);
        preparePrivateRoom(5);
        preparePrivateRoom(6);
        preparePrivateRoom(8);
        preparePrivateRoom(9);


        insertAndVerify(1);
        insertAndVerify(4);
        insertAndVerify(7);
        insertAndVerify(10);
        insertAndVerify(11);
    }

    private void insertAndVerify(int expectedNumber) {
        long guildId = uniqueLong();

        PrivateRoom inserted = this.repository.insert(guildId);

        assertThat(inserted).isNotNull();
        assertThat(inserted.getGuildId()).isEqualTo(guildId);
        assertThat(inserted.getNumber()).isEqualTo(expectedNumber);
    }

    private long preparePrivateRoom(int number) {
        long guildId = uniqueLong();

        this.jooq.transactionResult(config -> config.dsl()
                .insertInto(PRIVATE_ROOM)
                .columns(PRIVATE_ROOM.GUILD_ID, PRIVATE_ROOM.NUMBER)
                .values(guildId, number)
                .execute()
        );

        return guildId;
    }

}
