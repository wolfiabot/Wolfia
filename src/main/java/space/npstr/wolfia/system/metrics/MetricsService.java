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

package space.npstr.wolfia.system.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import org.springframework.stereotype.Service;

/**
 * Initializes and registers various metrics collectors
 */
@Service
public class MetricsService {

    public final Summary queryTime;
    public final Counter gamesPlayed;
    public final Summary commandRetentionTime;
    public final Summary commandProcessTime;
    /**
     * basically measurement of discord latency, however, ratelimiting  by the library is not accounted for (getting
     * ratelimited in a channel happens rather fast when users spam)
     */
    public final Summary commandResponseTime;
    public final Summary commandTotalTime;
    public final Gauge availablePrivateRooms;

    public MetricsService(CollectorRegistry registry) {
        this.queryTime = Summary.build()
                .name("query_time_seconds")
                .help("Time queries take")
                .labelNames("name") //identifier of the query, for example "activeUsers"
                .register(registry);
        this.gamesPlayed = Counter.build()
                .name("games_played_total")
                .help("Games Played")
                .labelNames("type", "mode")
                .register(registry);
        this.commandRetentionTime = Summary.build()
                .name("command_retention_seconds")
                .help("Time it takes from receiving a command till processing is started")
                .register(registry);
        this.commandProcessTime = Summary.build()
                .name("command_process_seconds")
                .help("Time the pure processing takes")
                .labelNames("command") //simple class name of the command
                .register(registry);
        this.commandResponseTime = Summary.build()
                .name("command_response_seconds")
                .help("Time it takes from replying till the user actually receives the answer")
                .register(registry);
        this.commandTotalTime = Summary.build()
                .name("command_total_seconds")
                .help("Total time it takes from discord creation timestamp of the trigger message till"
                        + " discord creation timestamp of the answer message")
                .register(registry);
        this.availablePrivateRooms = Gauge.build()
                .name("private_rooms_available")
                .help("Amount of available private rooms")
                .register(registry);
    }
}
