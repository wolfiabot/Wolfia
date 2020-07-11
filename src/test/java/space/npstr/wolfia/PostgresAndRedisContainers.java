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

package space.npstr.wolfia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Set up our testcontainers & pass their urls into the application config.
 * Yes, this is the recommended way to go about setting up containers that are shared by all tests.
 */
public abstract class PostgresAndRedisContainers {

    private static final GenericContainer<?> DB = new GenericContainer<>("napstr/wolfia-postgres:12")
            .withLogConsumer(new Slf4jLogConsumer(containerLogger("Postgres")))
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


    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:5-alpine")
            .withLogConsumer(new Slf4jLogConsumer(containerLogger("Redis")))
            .withExposedPorts(6379);

    static {
        REDIS.start();
        String host = REDIS.getContainerIpAddress();
        int port = REDIS.getMappedPort(6379);
        String redisUrl = "redis://" + host + ":" + port + "/1";
        System.setProperty("spring.redis.url", redisUrl);
    }

    protected static Logger containerLogger(String suffix) {
        return LoggerFactory.getLogger("Container." + suffix);
    }
}
