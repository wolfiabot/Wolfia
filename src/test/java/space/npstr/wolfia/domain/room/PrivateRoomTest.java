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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class PrivateRoomTest {

    @Test
    void getters() {
        long guildId = uniqueLong();
        int number = 42;
        PrivateRoom privateRoom = new PrivateRoom(guildId, number);

        assertThat(privateRoom.getGuildId()).isEqualTo(guildId);
        assertThat(privateRoom.getNumber()).isEqualTo(number);
    }

}
