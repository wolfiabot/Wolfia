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

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import com.github.napstr.logback.DiscordAppender;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.Requester;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.sqlsauce.ssh.SshTunnel;
import space.npstr.wolfia.charts.Charts;
import space.npstr.wolfia.commands.debug.SyncCommand;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.db.entities.EGuild;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.db.entities.stats.GeneralBotStats;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
import space.npstr.wolfia.listings.Listings;
import space.npstr.wolfia.utils.GitRepoState;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;
import space.npstr.wolfia.utils.log.LogTheStackException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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

    public static final long START_TIME = System.currentTimeMillis();
    public static final LinkedBlockingQueue<PrivateGuild> AVAILABLE_PRIVATE_GUILD_QUEUE = new LinkedBlockingQueue<>();
    public static final OkHttpClient defaultHttpClient = getDefaultHttpClientBuilder().build();
    //todo find a better way to execute tasks; java's built in ScheduledExecutorService is rather crappy for many reasons; until then a big-sized pool size will suffice to make sure tasks get executed when they are due
    public static final ExceptionLoggingExecutor executor = new ExceptionLoggingExecutor(100, "main-scheduled-executor");


    private static final Logger log = LoggerFactory.getLogger(Wolfia.class);
    private static ShardManager shardManager;

    private static boolean started = false;
    private static CommandListener commandListener;
    private static DatabaseWrapper dbWrapper;

    //set up things that are crucial
    //if something fails exit right away
    public static void main(final String[] args) throws InterruptedException {
        //just post the info to the console
        if (args.length > 0 &&
                (args[0].equalsIgnoreCase("-v")
                        || args[0].equalsIgnoreCase("--version")
                        || args[0].equalsIgnoreCase("-version"))) {
            System.out.println("Version flag detected. Printing version info, then exiting.");
            System.out.println(getVersionInfo());
            System.out.println("Version info printed, exiting.");
            return;
        }

        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);

        log.info(getVersionInfo());

        //add webhookURI to Discord log appender
        if (Config.C.errorLogWebHook != null && !"".equals(Config.C.errorLogWebHook)) {
            final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            final AsyncAppender discordAsync = (AsyncAppender) lc.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_DISCORD");
            final DiscordAppender disco = (DiscordAppender) discordAsync.getAppender("DISCORD");
            disco.setWebhookUri(Config.C.errorLogWebHook);
        }

        if (Config.C.isDebug)
            log.info("Running DEBUG configuration");
        else
            log.info("Running PRODUCTION configuration");

        //set up relational database
        try {
            final DatabaseConnection databaseConnection = new DatabaseConnection.Builder("postgres", Config.C.jdbcUrl)
                    .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                    .addEntityPackage("space.npstr.wolfia.db.entities")
                    .setAppName("Wolfia_" + (Config.C.isDebug ? "DEBUG" : "PROD") + "_" + App.VERSION)
                    .setSshDetails((Config.C.sshHost == null || Config.C.sshHost.isEmpty()) ? null :
                            new SshTunnel.SshDetails(Config.C.sshHost, Config.C.sshUser)
                                    .setLocalPort(Config.C.sshTunnelLocalPort)
                                    .setRemotePort(Config.C.sshTunnelRemotePort)
                                    .setKeyFile(Config.C.sshKeyFile)
                                    .setPassphrase(Config.C.sshKeyPassphrase)
                    )
//                    .addMigration(new m00001FixCharacterVaryingColumns())
//                    .addMigration(new m00002CachedUserToDiscordUser())
//                    .addMigration(new m00003EGuildToDiscordGuild())
                    .build();
            dbWrapper = new DatabaseWrapper(databaseConnection);
        } catch (final Exception e) {
            log.error("Failed to set up database connection, exiting", e);
            System.exit(2);
        }

        //fire up spark async
        executor.submit(Charts::spark);

        try {
            AVAILABLE_PRIVATE_GUILD_QUEUE.addAll(dbWrapper.selectJpqlQuery("FROM PrivateGuild", null, PrivateGuild.class));
            log.info("{} private guilds loaded", AVAILABLE_PRIVATE_GUILD_QUEUE.size());
        } catch (final DatabaseException e) {
            log.error("Failed to load private guilds, exiting", e);
            System.exit(2);
        }

        //set up JDA
        log.info("Setting up JDA and main listener");
        commandListener = new CommandListener();

//        int recommendedShardCount = 0;
//        while (recommendedShardCount < 1) {
//            try {
//                recommendedShardCount = getRecommendedShardCount(Config.C.discordToken);
//                log.info("Received recommended shard count: {}", recommendedShardCount);
//            } catch (final IOException e) {
//                log.error("Exception when getting recommended shard count, trying again in a bit", e);
//                Thread.sleep(5000);
//            }
//        }


        //create all necessary shards
        try {
            shardManager = new DefaultShardManagerBuilder()
                    .setToken(Config.C.discordToken)
                    .setGame(Game.playing(App.GAME_STATUS))
                    .addEventListeners(commandListener)
                    .addEventListeners(AVAILABLE_PRIVATE_GUILD_QUEUE.toArray())
                    .addEventListeners(new UserMemberCachingListener<>(CachedUser.class))
                    .addEventListeners(new GuildCachingListener<>(EGuild.class))
                    .addEventListeners(new InternalListener())
                    .addEventListeners(new Listings())
                    .addEventListeners(new WolfiaGuildListener())
                    .setHttpClientBuilder(getDefaultHttpClientBuilder())
                    .setEnableShutdownHook(false)
                    .setAudioEnabled(false)
                    .build();
        } catch (final Exception e) {
            log.error("could not create JDA object, possibly invalid bot token, exiting", e);
            return;
        }

        //wait for all shards to be online, then start doing things that expect the full bot to be online
        while (!allShardsUp()) {
            Thread.sleep(1000);
        }
        started = true;

        shardManager.getApplicationInfo().queue(
                App::setAppInfo,
                t -> log.error("Could not load application info", t));

        //post stats every 10 minutes
        executor.scheduleAtFixedRate(ExceptionLoggingExecutor.wrapExceptionSafe(Wolfia::generalBotStatsToDB),
                0, 10, TimeUnit.MINUTES);


        //sync guild cache
        // this takes a few seconds to do, so do it as the last thing of the main method, or put it into it's own thread
        SyncCommand.syncGuilds(executor, shardManager.getGuildCache().stream(), null);
        //user cache is not synced on each start as it takes a lot of time and resources. see SyncComm for manual triggering
    }

    private Wolfia() {
    }

    public static DatabaseWrapper getDbWrapper() {
        return dbWrapper;
    }

    public static CommandListener getCommandListener() {
        return commandListener;
    }

    private static void generalBotStatsToDB() throws DatabaseException {
        if (!started) {
            log.error("Skipping posting of bot stats due to not being ready yet");
            return;
        }
        log.info("Writing general bot stats to database");

        //noinspection ResultOfMethodCallIgnored
        dbWrapper.persist(new GeneralBotStats(
                getUsersAmount(),
                getGuildsAmount(),
                1,
                Games.getRunningGamesCount(),
                AVAILABLE_PRIVATE_GUILD_QUEUE.size(),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().maxMemory(),
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().availableProcessors(),
                ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
                System.currentTimeMillis() - START_TIME
        ));
    }

    /**
     * @return true if wolfia has started and all systems are expected to be operational
     */
    public static boolean isStarted() {
        return started;
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

    private static int getRecommendedShardCount(final String token) throws IOException {
        final Request request = new Request.Builder()
                .url(Requester.DISCORD_API_PREFIX + "gateway/bot")
                .header("Authorization", "Bot " + token)
                .header("user-agent", Requester.USER_AGENT)
                .build();
        try (final Response response = defaultHttpClient.newCall(request).execute()) {
            //noinspection ConstantConditions
            return new JSONObject(response.body().string()).getInt("shards");
        }
    }

    //returns a general purpose http client builder
    public static OkHttpClient.Builder getDefaultHttpClientBuilder() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
    }


    //################# shutdown handling

    public static final int EXIT_CODE_SHUTDOWN = 0;
    public static final int EXIT_CODE_RESTART = 2;

    private static boolean shuttingDown = false;

    public static boolean isShuttingDown() {
        return shuttingDown;
    }


    public static void shutdown(final int code) {
        DiscordLogger.getLogger().log("%s `%s` Shutting down with exit code %s",
                Emojis.DOOR, TextchatUtils.berlinTime(), code);
        log.info("Exiting with code {}", code);
        System.exit(code);
    }

    private static final Thread SHUTDOWN_HOOK = new Thread(() -> {
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

        log.info("Shutting down discord logger");
        DiscordLogger.shutdown(10, TimeUnit.SECONDS);

        //okHttpClient claims that a shutdown isn't necessary

        //shutdown JDA
        log.info("Shutting down shards");
        shardManager.shutdown();

        //shutdown executors
        log.info("Shutting down executor");
        final List<Runnable> runnables = executor.shutdownNow();
        log.info("{} runnables canceled", runnables.size());
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while awaiting executor termination");
        }

        //shutdown DB
        log.info("Shutting down database");
        dbWrapper.unwrap().shutdown();

        //shutdown logback logger
        log.info("Shutting down logger :rip:");
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }, "shutdown-hook");

    @Nonnull
    private static String getVersionInfo() {
        return art
                + "\n"
                + "\n\tVersion:       " + App.VERSION
                + "\n\tBuild:         " + App.BUILD_NUMBER
                + "\n\tBuild time:    " + TextchatUtils.toBerlinTime(App.BUILD_TIME)
                + "\n\tCommit:        " + GitRepoState.getGitRepositoryState().commitIdAbbrev + " (" + GitRepoState.getGitRepositoryState().branch + ")"
                + "\n\tCommit time:   " + TextchatUtils.toBerlinTime(GitRepoState.getGitRepositoryState().commitTime * 1000)
                + "\n\tJVM:           " + System.getProperty("java.version")
                + "\n\tJDA:           " + JDAInfo.VERSION
                + "\n";
    }

    //########## vanity
    private static final String art = "\n"
            + "\n                              __"
            + "\n                            .d$$b"
            + "\n                           .' TO$;\\"
            + "\n        Wolfia            /  : TP._;"
            + "\n    Werewolf & Mafia     / _.;  :Tb|"
            + "\n      Discord bot       /   /   ;j$j"
            + "\n                    _.-\"       d$$$$"
            + "\n                  .' ..       d$$$$;"
            + "\n                 /  /P'      d$$$$P. |\\"
            + "\n                /   \"      .d$$$P' |\\^\"l"
            + "\n              .'           `T$P^\"\"\"\"\"  :"
            + "\n          ._.'      _.'                ;"
            + "\n       `-.-\".-'-' ._.       _.-\"    .-\""
            + "\n     `.-\" _____  ._              .-\""
            + "\n    -(.g$$$$$$$b.              .'"
            + "\n      \"\"^^T$$$P^)            .(:"
            + "\n        _/  -\"  /.'         /:/;"
            + "\n     ._.'-'`-'  \")/         /;/;"
            + "\n  `-.-\"..--\"\"   \" /         /  ;"
            + "\n .-\" ..--\"\"        -'          :"
            + "\n ..--\"\"--.-\"         (\\      .-(\\"
            + "\n   ..--\"\"              `-\\(\\/;`"
            + "\n     _.                      :"
            + "\n                             ;`-"
            + "\n                            :\\"
            + "\n                            ;";
}
