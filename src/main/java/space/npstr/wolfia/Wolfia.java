/*
 * Copyright (C) 2017 Dennis Neufeld
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

import ch.qos.logback.classic.LoggerContext;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.hooks.EventListener;
import org.slf4j.LoggerFactory;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.wolfia.commands.debug.SyncCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.log.LogTheStackException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by npstr on 22.08.2016
 * <p>
 * Main class of Wolfia
 * //general list of todos etc
 * //todo rename role pm/dm -> rolecard
 */
public class Wolfia {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Wolfia.class);

    private static ShardManager shardManager;

    //set up things that are crucial
    //if something fails exit right away
    public static void start(ShardManager shardManager, ThreadPoolCollector poolMetrics,
                             ScheduledExecutorService jdaThreadPool, ExceptionLoggingExecutor executor) throws InterruptedException {
        Wolfia.shardManager = shardManager;
        Runtime.getRuntime().addShutdownHook(shutdownHook(jdaThreadPool, executor));

        poolMetrics.addPool("restActions", (ScheduledThreadPoolExecutor) RestActions.restService);

        final WolfiaConfig wolfiaConfig = Launcher.getBotContext().getWolfiaConfig();

        if (wolfiaConfig.isDebug())
            log.info("Running DEBUG configuration");
        else
            log.info("Running PRODUCTION configuration");

        //try connecting in a reasonable timeframe
        boolean dbConnected = false;
        final long dbConnectStarted = System.currentTimeMillis();
        do {
            try {
                //noinspection ResultOfMethodCallIgnored
                Launcher.getBotContext().getDatabase().getWrapper().selectSqlQuery("SELECT 1;", null);
                dbConnected = true;
                log.info("Initial db connection succeeded");
            } catch (final Exception e) {
                log.info("Failed initial db connection, retrying in a moment", e);
                Thread.sleep(1000);
            }
        } while (!dbConnected && System.currentTimeMillis() - dbConnectStarted < 1000 * 60 * 2); //2 minutes

        if (!dbConnected) {
            log.error("Failed to init db connection in a reasonable amount of time, exiting.");
            System.exit(2);
        }

        //wait for all shards to be online, then start doing things that expect the full bot to be online
        while (!allShardsUp()) {
            Thread.sleep(1000);
        }


        //sync guild cache
        // this takes a few seconds to do, so do it as the last thing of the main method, or put it into it's own thread
        SyncCommand.syncGuilds(shardManager, executor, shardManager.getGuildCache().stream(), null);
        //user cache is not synced on each start as it takes a lot of time and resources. see SyncComm for manual triggering
    }

    private Wolfia() {
    }

    // ########## JDA wrapper methods, they get 9000% more useful when sharding
    @Nullable
    public static Guild getGuildById(final long guildId) {
        return shardManager.getGuildById(guildId);
    }

    public static long getGuildsAmount() {
        return shardManager.getGuildCache().size();
    }

    @Nullable
    public static TextChannel getTextChannelById(final long channelId) {
        return shardManager.getTextChannelById(channelId);
    }

    //this method assumes that the id itself is legit and not a mistake
    // it is an attempt to improve the occasional inconsistency of discord which makes looking up entities a gamble
    // the main feature being the @Nonnull return contract, over the @Nullable contract of looking the entity up in JDA
    //todo what happens if we leave a server? do we get stuck in here? maybe make this throw an exception eventually and exit?
    @Nonnull
    public static TextChannel fetchTextChannel(final long channelId) {
        TextChannel tc = Wolfia.getTextChannelById(channelId);
        while (tc == null) {
            log.error("Could not find channel {}, retrying in a moment", channelId, new LogTheStackException());
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            tc = Wolfia.getTextChannelById(channelId);
        }
        return tc;
    }

    @Nullable
    public static User getUserById(final long userId) {
        return shardManager.getUserById(userId);
    }

    public static long getUsersAmount() {
        //UnifiedShardCacheViewImpl#stream calls distinct for us
        return shardManager.getUserCache().stream().count();
    }

    public static SelfUser getSelfUser() {
        return getFirstJda().getSelfUser();
    }

    public static void addEventListener(final EventListener eventListener) {
        shardManager.addEventListener(eventListener);
    }

    public static void removeEventListener(final EventListener eventListener) {
        shardManager.removeEventListener(eventListener);
    }

    public static long getResponseTotal() {
        return shardManager.getShards().stream().mapToLong(JDA::getResponseTotal).sum();
    }

    public static JDA getFirstJda() {
        return shardManager.getShards().iterator().next();
    }

    @Nonnull
    public static Collection<JDA> getShards() {
        return shardManager.getShards();
    }

    @Nonnull
    public static ShardManager getShardManager() {
        return shardManager;
    }

    public static boolean allShardsUp() {
        if (shardManager.getShards().size() < shardManager.getShardsTotal()) {
            return false;
        }
        for (final JDA jda : shardManager.getShards()) {
            if (jda.getStatus() != JDA.Status.CONNECTED) {
                return false;
            }
        }
        return true;
    }


    //################# shutdown handling

    public static final int EXIT_CODE_SHUTDOWN = 0;
    public static final int EXIT_CODE_RESTART = 2;

    private static boolean shuttingDown = false;

    public static boolean isShuttingDown() {
        return shuttingDown;
    }


    public static void shutdown(final int code) {
        log.info("Exiting with code {}", code);
        System.exit(code);
    }

    private static Thread shutdownHook(ScheduledExecutorService jdaThreadPool, ExceptionLoggingExecutor executor) {
        return new Thread(() -> {
            log.info("Shutdown hook triggered! {} games still ongoing.", Games.getRunningGamesCount());
            shuttingDown = true;
            Future waitForGamesToEnd = executor.submit(() -> {
                while (Games.getRunningGamesCount() > 0) {
                    log.info("Waiting on {} games to finish.", Games.getRunningGamesCount());
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignored) {
                    }
                }
            });
            try {
                //is this value is changed, make sure to adjust the one in docker-update.sh
                waitForGamesToEnd.get(2, TimeUnit.HOURS); //should be enough until the forseeable future
                //todo persist games (big changes)
            } catch (ExecutionException | InterruptedException | TimeoutException ignored) {
                log.error("dafuq", ignored);
            }
            if (Games.getRunningGamesCount() > 0) {
                log.error("Killing {} games while exiting", Games.getRunningGamesCount());
            }

            //shutdown JDA
            log.info("Shutting down shards");
            shardManager.shutdown();

            //shutdown executors
            log.info("Shutting down executor");
            final List<Runnable> runnables = executor.shutdownNow();
            log.info("{} runnables canceled", runnables.size());

            log.info("Shutting down jda thread pool");
            final List<Runnable> jdaThreadPoolRunnables = jdaThreadPool.shutdownNow();
            log.info("{} jda thread pool runnables canceled", jdaThreadPoolRunnables.size());

            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
                log.info("Main executor terminated");
                jdaThreadPool.awaitTermination(30, TimeUnit.SECONDS);
                log.info("Jda thread pool terminated");
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while awaiting executor termination");
            }

            //shutdown DB
            log.info("Shutting down database");
            Launcher.getBotContext().getDatabase().shutdown();

            //shutdown logback logger
            log.info("Shutting down logger :rip:");
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();
        }, "shutdown-hook");
    }
}
