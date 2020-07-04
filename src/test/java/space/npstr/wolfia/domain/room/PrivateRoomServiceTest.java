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

package space.npstr.wolfia.domain.room;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.Database;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;
import static space.npstr.wolfia.db.gen.Tables.PRIVATE_ROOM;

class PrivateRoomServiceTest extends ApplicationTest {

    @Autowired
    private PrivateRoomService service;

    @Autowired
    private PrivateRoomRepository repository;

    @Autowired
    private Database database;

    @BeforeEach
    @AfterEach
    void cleanDbTable() {
        this.database.getJooq().transactionResult(config -> DSL.using(config)
                .deleteFrom(PRIVATE_ROOM)
                .execute()
        );
    }

    @Test
    void givenNoPrivateRooms_returnNoRooms() {
        List<PrivateRoom> rooms = this.service.getAll();

        assertThat(rooms).isEmpty();
    }

    @Test
    void givenPrivateRooms_returnRooms() {
        long guildIdA = uniqueLong();
        long guildIdB = uniqueLong();
        this.repository.insert(guildIdA).toCompletableFuture().join();
        this.repository.insert(guildIdB).toCompletableFuture().join();

        List<PrivateRoom> rooms = this.service.getAll();

        assertThat(rooms).hasSize(2);
        assertThat(rooms).filteredOnAssertions(isGuild(guildIdA)).hasSize(1);
        assertThat(rooms).filteredOnAssertions(isGuild(guildIdB)).hasSize(1);
    }

    @Test
    void givenGuildIsNotAPrivateRoom_returnIsPrivateRoomFalse() {
        long guildId = uniqueLong();

        boolean isPrivateRoom = this.service.guild(guildId).isPrivateRoom();

        assertThat(isPrivateRoom).isFalse();
    }

    @Test
    void givenGuildIsAPrivateRoom_returnIsPrivateRoomTrue() {
        long guildId = uniqueLong();
        this.repository.insert(guildId).toCompletableFuture().join();

        boolean isPrivateRoom = this.service.guild(guildId).isPrivateRoom();

        assertThat(isPrivateRoom).isTrue();
    }

    @Test
    void whenRegisterGuildThatIsNotAPrivateRoom_registerReturnsPrivateRoom() {
        long guildId = uniqueLong();

        Optional<PrivateRoom> registered = this.service.guild(guildId).register();

        assertThat(registered).hasValueSatisfying(room -> assertThat(room.getGuildId()).isEqualTo(guildId));
    }

    @Test
    void whenRegisterGuildThatIsAPrivateRoom_registerReturnsEmpty() {
        long guildId = uniqueLong();
        this.repository.insert(guildId).toCompletableFuture().join();

        Optional<PrivateRoom> registered = this.service.guild(guildId).register();

        assertThat(registered).isEmpty();
    }

    private Consumer<PrivateRoom> isGuild(long guildId) {
        return actual -> assertThat(actual.getGuildId()).isEqualTo(guildId);
    }

}
