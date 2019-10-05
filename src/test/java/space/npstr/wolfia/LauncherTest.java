/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherTest extends ApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextLoads() {
        // smoke test for some usual & important beans
        assertTrue(this.applicationContext.containsBean("commandListener"));
        assertTrue(this.applicationContext.containsBean("commRegistry"));
        assertTrue(this.applicationContext.containsBean("shardManager"));
        assertTrue(this.applicationContext.containsBean("botContext"));
        assertTrue(this.applicationContext.containsBean("database"));
    }

}
