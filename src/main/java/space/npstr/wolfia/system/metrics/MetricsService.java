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

package space.npstr.wolfia.system.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

/**
 * Initializes and registers various metrics collectors
 */
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private final Map<String, Map<Tags, AtomicInteger>> cache = new ConcurrentHashMap<>();

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private AtomicInteger gauge(String name, Tags tags, Consumer<Gauge.Builder<AtomicInteger>> registrar) {
        return cache
                .computeIfAbsent(name, __ -> new ConcurrentHashMap<>())
                .computeIfAbsent(tags, __ -> {
                    AtomicInteger atomicInteger = new AtomicInteger();
                    registrar.accept(Gauge.builder(name, atomicInteger, AtomicInteger::get).tags(tags));
                    return atomicInteger;
                });
    }

    public Timer queryTime(String name) {
        return Timer.builder("query.time")
                .description("Time queries take")
                .tag("name", name) //identifier of the query, for example "activeUsers"
                .register(meterRegistry);
    }

    public Counter gamesPlayed(Games type, GameInfo.GameMode mode) {
        return Counter.builder("games.played")
                .description("Games Played")
                .tag("type", type.name())
                .tag("mode", mode.name())
                .register(meterRegistry);
    }

    public Timer commandRetentionTime() {
        return Timer.builder("command.retention")
                .description("Time it takes from receiving a command till processing is started")
                .register(meterRegistry);
    }

    public Timer commandProcessTime(String command) {
        return Timer.builder("command.process")
                .description("Time the pure processing takes")
                .tag("command", command) //simple class name of the command
                .register(meterRegistry);
    }

    /**
     * basically measurement of discord latency, however, ratelimiting  by the library is not accounted for (getting
     * ratelimited in a channel happens rather fast when users spam)
     */
    public Timer commandResponseTime() {
        return Timer.builder("command.response")
                .description("Time it takes from replying till the user actually receives the answer")
                .register(meterRegistry);
    }

    public Timer commandTotalTime() {
        return Timer.builder("command.total.time")
                .description("Total time it takes from discord creation timestamp of the trigger message till"
                        + " discord creation timestamp of the answer message")
                .register(meterRegistry);
    }

    public AtomicInteger availablePrivateRooms() {
        return gauge("private.rooms.available", Tags.empty(), builder -> builder
                .description("Amount of available private rooms")
                .register(meterRegistry)
        );
    }
}
