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

import io.prometheus.client.CollectorRegistry;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.config.ShardManagerFactory;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
import space.npstr.wolfia.system.redis.Redis;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;

@Component
public class ShutdownHandler implements ApplicationListener<ContextClosedEvent> {

    public static final int EXIT_CODE_RESTART = 2;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShutdownHandler.class);
    private static final Thread DUMMY_HOOK = new Thread("dummy-hook");
    private final ExceptionLoggingExecutor executor;
    private final Database database;
    private final AsyncDbWrapper dbWrapper;
    private final ShardManagerFactory shardManagerFactory;
    private final GameRegistry gameRegistry;
    private final Redis redis;
    private final ScheduledExecutorService jdaThreadPool;

    private boolean shuttingDown = false;

    public ShutdownHandler(ExceptionLoggingExecutor executor, Database database, AsyncDbWrapper dbWrapper,
                           ShardManagerFactory shardManagerFactory, GameRegistry gameRegistry, Redis redis,
                           @Qualifier("jdaThreadPool") ScheduledExecutorService jdaThreadPool) {
        this.executor = executor;
        this.database = database;
        this.dbWrapper = dbWrapper;
        this.shardManagerFactory = shardManagerFactory;
        this.gameRegistry = gameRegistry;
        this.redis = redis;
        this.jdaThreadPool = jdaThreadPool;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shutdown();
    }

    private void shutdown() {
        this.shuttingDown = true;
        log.info("Shutdown hook triggered! {} games still ongoing.", gameRegistry.getRunningGamesCount());
        Future<?> waitForGamesToEnd = executor.submit(() -> {
            while (gameRegistry.getRunningGamesCount() > 0) {
                log.info("Waiting on {} games to finish.", gameRegistry.getRunningGamesCount());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        try {
            //if this value is changed, make sure to adjust the one in docker-update.sh
            waitForGamesToEnd.get(2, TimeUnit.HOURS); //should be enough until the forseeable future
            //todo persist games (big changes)
        } catch (InterruptedException e) {
            log.warn("Interrupted while awaiting games to be finished", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ignored) {
            // ignored
        }

        if (gameRegistry.getRunningGamesCount() > 0) {
            log.error("Killing {} games while exiting", gameRegistry.getRunningGamesCount());
            String reason = "Wolfia is shutting down. I'll probably be right back!";
            gameRegistry.getAll().values().forEach(game -> game.destroy(new UserFriendlyException(reason)));
        }

        //okHttpClient claims that a shutdown isn't necessary

        //shutdown JDA
        log.info("Shutting down shards");
        shardManagerFactory.shutdown();

        //shutdown executors
        log.info("Shutting down main executor");
        final List<Runnable> executorRunnables = executor.shutdownNow();
        log.info("{} main executor runnables cancelled", executorRunnables.size());

        log.info("Shutting down rest actions executor");
        final ScheduledExecutorService restService = RestActions.restService;
        final List<Runnable> restActionsRunnables = restService.shutdownNow();
        log.info("{} rest actions runnables cancelled", restActionsRunnables.size());

        log.info("Shutting down jda thread pool");
        final List<Runnable> jdaThreadPoolRunnables = jdaThreadPool.shutdownNow();
        log.info("{} jda thread pool runnables cancelled", jdaThreadPoolRunnables.size());

        log.info("Shutting down async database executor");
        final List<Runnable> dbWrapperRunnables = dbWrapper.shutdownNow();
        log.info("{} async database executor runnable cancelled", dbWrapperRunnables.size());

        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
            log.info("Main executor terminated");
            restService.awaitTermination(30, TimeUnit.SECONDS);
            log.info("Rest service terminated");
            jdaThreadPool.awaitTermination(30, TimeUnit.SECONDS);
            log.info("Jda thread pool terminated");
        } catch (final InterruptedException e) {
            log.warn("Interrupted while awaiting executors termination", e);
            Thread.currentThread().interrupt();
        }

        //shutdown DB connection
        log.info("Shutting down database connection");
        database.shutdown();

        //shutdown Redis connection
        log.info("Shutting down redis connection");
        redis.shutdown();

        //avoid trouble with spring dev tools, see https://github.com/prometheus/client_java/issues/279#issuecomment-335817904
        CollectorRegistry.defaultRegistry.clear();
    }

    public void shutdown(final int code) {
        log.info("Exiting with code {}", code);
        System.exit(code);
    }

    public boolean isShuttingDown() {
        if (shuttingDown) return true;
        try {
            Runtime.getRuntime().addShutdownHook(DUMMY_HOOK);
            Runtime.getRuntime().removeShutdownHook(DUMMY_HOOK);
        } catch (IllegalStateException ignored) {
            return true;
        }
        return false;
    }
}

