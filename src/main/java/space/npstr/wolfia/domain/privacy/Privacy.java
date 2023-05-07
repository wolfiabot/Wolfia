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

package space.npstr.wolfia.domain.privacy;

import java.beans.ConstructorProperties;

public class Privacy {

    private final long userId;
    private final boolean processData;

    @ConstructorProperties({"userId", "processData"})
    public Privacy(long userId, boolean processData) {
        this.userId = userId;
        this.processData = processData;
    }

    public long getUserId() {
        return this.userId;
    }

    public boolean isProcessData() {
        return this.processData;
    }

}
