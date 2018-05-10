/*
 * Copyright (C) 2017-2018 Dennis Neufeld
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

package space.npstr.wolfia;

import org.springframework.stereotype.Component;
import space.npstr.wolfia.db.Database;

/**
 * Created by napster on 10.05.18.
 * <p>
 * Temporary uber class that allows resources that were previously accessed statically to continue to be accessed
 * that way through {@link Launcher#getBotContext()}, until the whole project is refactored into non-static components.
 * <p>
 * todo resolve this temporary file
 */
@Component
public class BotContext {

    private final Database database;

    public BotContext(final Database database) {
        this.database = database;
    }

    public Database getDatabase() {
        return this.database;
    }
}
