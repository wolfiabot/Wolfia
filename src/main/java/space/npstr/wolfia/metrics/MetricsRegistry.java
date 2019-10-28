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

package space.npstr.wolfia.metrics;

import ch.qos.logback.classic.LoggerContext;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.logback.InstrumentedAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.QueryCountCollector;
import space.npstr.prometheus_extensions.ThreadPoolCollector;

/**
 * Initializes and registers various metrics collectors
 */
@Component
public class MetricsRegistry {

    public MetricsRegistry(InstrumentedAppender prometheusAppender, ThreadPoolCollector poolMetrics,
                           QueryCountCollector queryMetrics) {
        //log metrics
        final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
        prometheusAppender.setContext(root.getLoggerContext());
        prometheusAppender.start();
        root.addAppender(prometheusAppender);

        //jvm (hotspot) metrics
        DefaultExports.initialize();

        poolMetrics.register();
        queryMetrics.register();
    }

    public CollectorRegistry getRegistry() {
        return CollectorRegistry.defaultRegistry;
    }

    public static final Summary queryTime = Summary.build()
            .name("query_time_seconds")
            .help("Time queries take")
            .labelNames("name") //identifier of the query, for example "activeUsers"
            .register();

    public static final Counter gamesPlayed = Counter.build()
            .name("games_played")
            .help("Games Played")
            .labelNames("type", "mode")
            .register();
}
