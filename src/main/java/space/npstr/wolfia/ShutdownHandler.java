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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.discordwrapper.DiscordEntityProvider;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by napster on 26.09.18.
 */
@Component
public class ShutdownHandler {

    public static final int EXIT_CODE_RESTART = 2;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShutdownHandler.class);

    private volatile boolean shutdownHookAdded;
    private volatile boolean shutdownHookExecuted = false;

    public ShutdownHandler(ExceptionLoggingExecutor executor, Database database, AsyncDbWrapper dbWrapper,
                           DiscordEntityProvider discordEntityProvider, ScheduledExecutorService jdaThreadPool) {

        this.shutdownHookAdded = false;
        Thread shutdownHook = new Thread(() -> {
            try {
                shutdown(executor, jdaThreadPool, discordEntityProvider,
                        database, dbWrapper);
            } catch (Exception e) {
                log.error("Uncaught exception in shutdown hook", e);
            } finally {
                this.shutdownHookExecuted = true;
            }
        }, "shutdown-hook");

        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.shutdownHookAdded = true;
    }

    private void shutdown(ExceptionLoggingExecutor executor,
                          @Qualifier("jdaThreadPool") ScheduledExecutorService jdaThreadPool,
                          DiscordEntityProvider discordEntityProvider, Database database, AsyncDbWrapper dbWrapper) {

        log.info("Shutdown hook triggered! {} games still ongoing.", Games.getRunningGamesCount());
        Future waitForGamesToEnd = executor.submit(() -> {
            while (Games.getRunningGamesCount() > 0) {
                log.info("Waiting on {} games to finish.", Games.getRunningGamesCount());
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

        if (Games.getRunningGamesCount() > 0) {
            log.error("Killing {} games while exiting", Games.getRunningGamesCount());
            String reason = "Wolfia is shutting down. I'll probably be right back!";
            Games.getAll().values().forEach(game -> game.destroy(new UserFriendlyException(reason)));
        }

        //okHttpClient claims that a shutdown isn't necessary

        //shutdown JDA
        log.info("Shutting down shards");
        discordEntityProvider.shutdown();

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
    }

    @PreDestroy
    public void waitOnShutdownHook() {

        // This condition can happen when spring encountered an exception during start up and is tearing itself down,
        // but did not call System.exit, so out shutdown hooks are not being executed.
        // If spring is tearing itself down, we always want to exit the JVM, so we call System.exit manually here, so
        // our shutdown hooks will be run, and the loop below does not hang forever.
        if (!isShuttingDown()) {
            System.exit(1);
        }

        while (this.shutdownHookAdded && !this.shutdownHookExecuted) {
            log.info("Waiting on shutdown hook to be done...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Main shutdown hook done! Proceeding.");
    }

    private static final Thread DUMMY_HOOK = new Thread();

    public static void shutdown(final int code) {
        log.info("Exiting with code {}", code);
        System.exit(code);
    }

    public static boolean isShuttingDown() {
        try {
            Runtime.getRuntime().addShutdownHook(DUMMY_HOOK);
            Runtime.getRuntime().removeShutdownHook(DUMMY_HOOK);
        } catch (IllegalStateException ignored) {
            return true;
        }
        return false;
    }
}

