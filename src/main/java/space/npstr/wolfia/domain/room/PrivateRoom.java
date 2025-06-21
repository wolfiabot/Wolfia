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

import java.beans.ConstructorProperties;

/**
 * Represents private guilds or similar that can be used for wolf chat etc.
 */
public class PrivateRoom {

    private final long guildId;
    private final int number;

    @ConstructorProperties({"guildId", "number"})
    public PrivateRoom(long guildId, int number) {
        this.guildId = guildId;
        this.number = number;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "PrivateRoom{" +
                "guildId=" + guildId +
                ", number=" + number +
                '}';
    }
}
