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

package space.npstr.wolfia.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;

@Component
public class Redis {

    private static final Logger log = LoggerFactory.getLogger(Redis.class);

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final StatefulRedisPubSubConnection<String, String> pubSub;

    public Redis(RedisProperties redisProperties) {
        this.client = RedisClient.create(redisProperties.getUrl());
        this.connection = this.client.connect();
        this.pubSub = this.client.connectPubSub();

        this.client.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof ConnectionActivatedEvent) {
                enableExpirationEvents();
            }
        });
        enableExpirationEvents();
    }

    private void enableExpirationEvents() {
        // see https://redis.io/topics/notifications
        this.pubSub.async().configSet("notify-keyspace-events", "Ex")
                .thenAccept(configResult -> {
                    if (!"OK".equals(configResult)) {
                        log.warn("Failed to update redis config: {}", configResult);
                    }
                })
                .whenComplete((v, t) -> {
                    if (t != null) {
                        log.warn("Failed to update redis config", t);
                    }
                });
    }

    public StatefulRedisConnection<String, String> getConnection() {
        return this.connection;
    }

    public StatefulRedisPubSubConnection<String, String> getPubSub() {
        return this.pubSub;
    }

    public void shutdown() {
        this.connection.close();
        this.client.shutdown();
    }
}
