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

package space.npstr.wolfia.config;

import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
import io.prometheus.client.logback.InstrumentedAppender;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.prometheus_extensions.QueryCountCollector;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.prometheus_extensions.jda.JdaMetrics;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class MetricsConfiguration {

    //caffeine cache metrics
    @Bean
    public CacheMetricsCollector cacheMetrics() {
        return new CacheMetricsCollector().register();
    }

    @Bean
    public InstrumentedAppender instrumentedAppender() {
        return new InstrumentedAppender();
    }

    @Bean
    public SingleQueryCountHolder queryCountHolder() {
        return new SingleQueryCountHolder();
    }

    @Bean
    public QueryCountCollector queryCountCollector(SingleQueryCountHolder queryCountHolder) {
        return new QueryCountCollector(queryCountHolder);
    }

    @Bean
    public ThreadPoolCollector threadPoolCollector() {
        return new ThreadPoolCollector();
    }

    @Bean
    public JdaMetrics jdaMetrics(ShardManager shardManager, @Qualifier("jdaThreadPool") ScheduledExecutorService scheduler) {
        return new JdaMetrics(shardManager, scheduler);
    }
}
