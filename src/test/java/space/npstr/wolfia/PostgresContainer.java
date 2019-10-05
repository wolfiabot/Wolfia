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

import org.testcontainers.containers.GenericContainer;

/**
 * Set up our database testcontainer & pass its jdbcurl into the application config.
 * Yes, this is the recommended way to go about setting up a container that is shared by all tests.
 */
public abstract class PostgresContainer {

    private static final GenericContainer<?> DB = new GenericContainer<>("napstr/wolfia-postgres:11")
            .withEnv("ROLE", "wolfia_test")
            .withEnv("DB", "wolfia_test")
            .withExposedPorts(5432);

    static {
        DB.start();
        String host = DB.getContainerIpAddress();
        int port = DB.getMappedPort(5432);
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/wolfia_test?user=wolfia_test";
        System.setProperty("database.jdbcUrl", jdbcUrl);
    }
}
