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

package space.npstr.wolfia.db;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.wolfia.common.Exceptions;
import space.npstr.wolfia.config.properties.DatabaseConfig;

/**
 * JDBC is blocking at its core. This class wraps calls to it into a thread pool.
 */
@Component
public class AsyncDbWrapper {

    private final Database database;
    private final ScheduledThreadPoolExecutor executor;

    public AsyncDbWrapper(Database database, DatabaseConfig databaseConfig, ThreadPoolCollector threadPoolMetrics) {
        this.database = database;
        final var threadCounter = new AtomicInteger();
        this.executor = new ScheduledThreadPoolExecutor(databaseConfig.getAsyncPoolSize(),
                r -> {
                    Thread t = new Thread(r, "database-executor-t" + threadCounter.getAndIncrement());
                    t.setUncaughtExceptionHandler(Exceptions.UNCAUGHT_EXCEPTION_HANDLER);
                    return t;
                });

        threadPoolMetrics.addPool("database", this.executor);
    }


    @CheckReturnValue
    public <E> CompletionStage<E> jooq(Function<DSLContext, E> databaseOperation) {
        return CompletableFuture.supplyAsync(
                () -> databaseOperation.apply(this.database.jooq()),
                this.executor
        );
    }

    public List<Runnable> shutdownNow() {
        return this.executor.shutdownNow();
    }
}
