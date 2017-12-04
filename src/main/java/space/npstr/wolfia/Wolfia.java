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
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.Requester;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.entities.discord.DiscordGuild;
import space.npstr.sqlsauce.entities.discord.DiscordUser;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.sqlsauce.ssh.SshTunnel;
import space.npstr.wolfia.charts.Charts;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.db.entities.EGuild;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.db.entities.stats.GeneralBotStats;
import space.npstr.wolfia.db.entities.stats.MessageOutputStats;
import space.npstr.wolfia.db.migrations.m00001FixCharacterVaryingColumns;
import space.npstr.wolfia.db.migrations.m00002CachedUserToDiscordUser;
import space.npstr.wolfia.db.migrations.m00003EGuildToDiscordGuild;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
import space.npstr.wolfia.listings.Listings;
import space.npstr.wolfia.utils.GitRepoState;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;
import space.npstr.wolfia.utils.log.LogTheStackException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by npstr on 22.08.2016
 * <p>
 * Main class of Wolfia
 */
public class Wolfia {

    public static final long START_TIME = System.currentTimeMillis();
    public static final LinkedBlockingQueue<PrivateGuild> AVAILABLE_PRIVATE_GUILD_QUEUE = new LinkedBlockingQueue<>();
    public static final OkHttpClient defaultHttpClient = getDefaultHttpClientBuilder().build();
    //todo find a better way to execute tasks; java's built in ScheduledExecutorService is rather crappy for many reasons; until then a big-sized pool size will suffice to make sure tasks get executed when they are due
    public static final ExceptionLoggingExecutor executor = new ExceptionLoggingExecutor(100, "main-scheduled-executor");


    private static final Logger log = LoggerFactory.getLogger(Wolfia.class);
    private static final Set<JDA> jdas = ConcurrentHashMap.newKeySet();
    private static final SessionReconnectQueue sessionReconnectQueue = new SessionReconnectQueue();

    private static boolean started = false;
    private static CommandListener commandListener;
    private static DatabaseWrapper dbWrapper;

    // will print a proper stack trace for exceptions happening in queue(), showing the code leading up to the call of
    // the queue() that failed
    public static Consumer<Throwable> defaultOnFail() {
        final LogTheStackException ex = new LogTheStackException();
        return t -> {
            ex.initCause(t);
            log.error("Exception during queue(): {}", t.getMessage(), ex);
        };
    }

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
                    .addMigration(new m00001FixCharacterVaryingColumns())
                    .addMigration(new m00002CachedUserToDiscordUser())
                    .addMigration(new m00003EGuildToDiscordGuild())
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
        commandListener = new CommandListener(AVAILABLE_PRIVATE_GUILD_QUEUE.stream()
                .map(PrivateGuild::getId)
                .collect(Collectors.toList())
        );

        int recommendedShardCount = 0;
        while (recommendedShardCount < 1) {
            try {
                recommendedShardCount = getRecommendedShardCount(Config.C.discordToken);
                log.info("Received recommended shard count: {}", recommendedShardCount);
            } catch (final IOException e) {
                log.error("Exception when getting recommended shard count, trying again in a bit", e);
                Thread.sleep(5000);
            }
        }

        final JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
                .setToken(Config.C.discordToken)
                .addEventListener(commandListener)
                .addEventListener(AVAILABLE_PRIVATE_GUILD_QUEUE.toArray())
                .addEventListener(new UserMemberCachingListener<>(CachedUser.class))
                .addEventListener(new GuildCachingListener<>(EGuild.class))
                .addEventListener(new InternalListener())
                .addEventListener(new Listings())
                .setEnableShutdownHook(false)
                .setGame(Game.of(App.GAME_STATUS))
                .setHttpClientBuilder(getDefaultHttpClientBuilder())
                .setReconnectQueue(sessionReconnectQueue);

        //create all necessary shards
        try {
            for (int i = 0; i < recommendedShardCount; i++) {
                jdaBuilder.useSharding(i, recommendedShardCount);
                log.info("Building shard {} of {}", i, recommendedShardCount);
                final JDA jda = jdaBuilder.buildAsync();
                if (i == 0) {
                    jda.asBot().getApplicationInfo().queue(
                            App::setAppInfo,
                            t -> log.error("Could not load application info", t));
                }
                jdas.add(jda);
                Thread.sleep(5500);

                if (Config.C.isDebug) {
                    break; //one shard is enough, we can talk to it via DMs
                }
            }
        } catch (final Exception e) {
            log.error("could not create JDA object, possibly invalid bot token, exiting", e);
            return;
        }


        //post stats every 10 minutes
        executor.scheduleAtFixedRate(ExceptionLoggingExecutor.wrapExceptionSafe(Wolfia::generalBotStatsToDB),
                1, 10, TimeUnit.MINUTES);
        started = true;
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
        for (final JDA jda : jdas) {
            final Guild guild = jda.getGuildById(guildId);
            if (guild != null) return guild;
        }
        return null;
    }

    public static long getGuildsAmount() {
        return jdas.stream().mapToLong(jda -> jda.getGuildCache().size()).sum();
    }

    @Nullable
    public static TextChannel getTextChannelById(final long channelId) {
        for (final JDA jda : jdas) {
            final TextChannel textChannel = jda.getTextChannelById(channelId);
            if (textChannel != null) return textChannel;
        }
        return null;
    }

    @Nullable
    public static User getUserById(final long userId) {
        for (final JDA jda : jdas) {
            final User user = jda.getUserById(userId);
            if (user != null) return user;
        }
        return null;
    }

    public static long getUsersAmount() {
        return jdas.stream().flatMap(jda -> jda.getUsers().stream()).distinct().count();
    }

    public static SelfUser getSelfUser() {
        return getFirstJda().getSelfUser();
    }

    public static void addEventListener(final EventListener eventListener) {
        for (final JDA jda : jdas) {
            jda.addEventListener(eventListener);
        }
    }

    public static void removeEventListener(final EventListener eventListener) {
        for (final JDA jda : jdas) {
            jda.removeEventListener(eventListener);
        }
    }

    public static long getResponseTotal() {
        return jdas.stream().mapToLong(JDA::getResponseTotal).sum();
    }

    public static JDA getFirstJda() {
        return jdas.iterator().next();
    }

    public static long getTotalGuildSize() {
        return Wolfia.jdas.stream().mapToLong(jda -> jda.getGuildCache().size()).sum();
    }

    public static boolean allShardsUp() {
        for (final JDA jda : jdas) {
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
                .header("User-agent", Requester.USER_AGENT)
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

    //################## message handling + tons of overloaded methods

    //calling with complete = true will ignore onsuccess and on fail, but return an optional with the message
    private static Optional<Message> handleOutputMessage(final boolean complete, final MessageChannel channel, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        if (complete && (onSuccess != null || onFail != null)) {
            log.warn("called handleOutputMessage() with complete set to true AND an onSuccess or onFail handler. check your code, dude");
        }
        if (channel == null) {
            throw new IllegalArgumentException("Provided channel is null");
        }
        final MessageBuilder mb = new MessageBuilder();
        if (args.length == 0) {
            mb.append(msg);
        } else {
            mb.appendFormat(msg, args);
        }
        try {
            final RestAction<Message> ra = channel.sendMessage(mb.build());
            if (complete) {
                final Message message = ra.complete();
                executor.submit(() -> dbWrapper.persist(new MessageOutputStats(message)));
                return Optional.of(message);
            } else {
                Consumer<Throwable> fail = onFail;
                if (fail == null) {
                    fail = throwable -> {
                        if (!(channel instanceof PrivateChannel)) { //ignore exceptions when sending to private channels
                            final LogTheStackException stack = new LogTheStackException();
                            stack.initCause(throwable);
                            log.error("Exception when sending a message in channel {}", channel.getIdLong(), stack);
                        }
                    };
                }
                //for stats keeping
                final Consumer<Message> wrappedSuccess = (message) -> {
                    executor.submit(() -> dbWrapper.persist(new MessageOutputStats(message)));
                    if (onSuccess != null) onSuccess.accept(message);
                };
                ra.queue(wrappedSuccess, fail);
            }
        } catch (final PermissionException e) {
            log.error("Could not post a message in channel {} due to missing permission {}", channel.getId(), e.getPermission().name(), e);
            if (onFail != null) onFail.accept(e);
        }
        return Optional.empty();
    }

    public static Optional<Message> handleOutputMessage(final boolean complete, final MessageChannel channel, final String msg, final Object... args) {
        return handleOutputMessage(complete, channel, null, null, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final boolean complete, final long channelId, final String msg, final Object... args) {
        final TextChannel channel = getTextChannelById(channelId);
        return handleOutputMessage(complete, channel, null, null, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final MessageChannel channel, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        return handleOutputMessage(false, channel, onSuccess, onFail, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final long channelId, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        final TextChannel channel = getTextChannelById(channelId);
        return handleOutputMessage(channel, onSuccess, onFail, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final MessageChannel channel, final String msg, final Object... args) {
        return handleOutputMessage(false, channel, null, null, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final long channelId, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        final TextChannel channel = getTextChannelById(channelId);
        return handleOutputMessage(false, channel, null, onFail, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final long channelId, final String msg, final Object... args) {
        return handleOutputMessage(channelId, null, msg, args);
    }

    //embeds
    private static Optional<Message> handleOutputEmbed(final boolean complete, final MessageChannel channel,
                                                       final MessageEmbed msgEmbed, final Consumer<Message> onSuccess,
                                                       final Consumer<Throwable> onFail) {
        //check for embed permissions in a guild text channel
        if (channel instanceof TextChannel) {
            final TextChannel tc = (TextChannel) channel;
            RoleAndPermissionUtils.acquireChannelPermissions(tc, Permission.MESSAGE_EMBED_LINKS);
        }
        try {
            final RestAction<Message> ra = channel.sendMessage(msgEmbed);
            if (complete) {
                final Message message = ra.complete();
                executor.submit(() -> dbWrapper.persist(new MessageOutputStats(message)));
                return Optional.of(message);
            } else {
                //for stats keeping
                final Consumer<Message> wrappedSuccess = (message) -> {
                    executor.submit(() -> dbWrapper.persist(new MessageOutputStats(message)));
                    if (onSuccess != null) onSuccess.accept(message);
                };
                ra.queue(wrappedSuccess, onFail != null ? onFail : defaultOnFail());
            }
        } catch (final PermissionException e) {
            log.error("Could not post a message in channel {} due to missing permission {}", channel.getId(), e.getPermission().name(), e);
        }
        return Optional.empty();
    }

    public static Optional<Message> handleOutputEmbed(final MessageChannel channel, final MessageEmbed msgEmbed) {
        return handleOutputEmbed(false, channel, msgEmbed, null, null);
    }

    public static Optional<Message> handleOutputEmbed(final boolean complete, final long channelId, final MessageEmbed msgEmbed) {
        final TextChannel channel = getTextChannelById(channelId);
        return handleOutputEmbed(complete, channel, msgEmbed, null, null);
    }

    public static Optional<Message> handleOutputEmbed(final long channelId, final MessageEmbed msgEmbed) {
        return handleOutputEmbed(false, channelId, msgEmbed);
    }

    public static Optional<Message> handleOutputEmbed(final MessageChannel channel, final MessageEmbed msgEmbed, final Consumer<Message> onSuccess) {
        return handleOutputEmbed(false, channel, msgEmbed, onSuccess, null);
    }


    //send a message to a user privately
    public static void handlePrivateOutputMessage(final long userId, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        if (onFail == null) {
            log.error("Trying to send a private message without an onFail handler :smh:. This may lead to unnecessary " +
                    "error log spam. Fix your code please.");
        }
        try {
            final User u = getUserById(userId);
            if (u == null) {
                throw new NullPointerException("No such user: " + userId);
            }
            u.openPrivateChannel().queue((privateChannel) -> Wolfia.handleOutputMessage(privateChannel, null, onFail, msg, args), onFail);
        } catch (final Exception e) {
            if (onFail != null) onFail.accept(e);
        }
    }

    public static void handlePrivateOutputMessage(final long userId, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        if (onFail == null) {
            log.error("Trying to send a private message without an onFail handler :smh:. This may lead to unnecessary " +
                    "error log spam. Fix your code please.");
        }
        try {
            final User u = getUserById(userId);
            if (u == null) {
                throw new NullPointerException("No such user: " + userId);
            }
            u.openPrivateChannel().queue((privateChannel) -> Wolfia.handleOutputMessage(privateChannel, onSuccess, onFail, msg, args), onFail);
        } catch (final Exception e) {
            if (onFail != null) onFail.accept(e);
        }
    }

    public static void handlePrivateOutputEmbed(final long userId, final Consumer<Throwable> onFail, final MessageEmbed messageEmbed) {
        if (onFail == null) {
            log.error("Trying to send a private message without an onFail handler :smh:. This may lead to unnecessary " +
                    "error log spam. Fix your code please.");
        }
        try {
            final User u = getUserById(userId);
            if (u == null) {
                throw new NullPointerException("No such user: " + userId);
            }
            u.openPrivateChannel().queue((privateChannel -> Wolfia.handleOutputEmbed(false, privateChannel, messageEmbed, null, onFail)));
        } catch (final Exception e) {
            if (onFail != null) onFail.accept(e);
        }
    }


    //################# end of message handling

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
        }
        if (Games.getRunningGamesCount() > 0) {
            log.error("Killing {} games while exiting", Games.getRunningGamesCount());
        }

        log.info("Shutting down discord logger");
        DiscordLogger.shutdown(10, TimeUnit.SECONDS);

        //okHttpClient claims that a shutdown isn't necessary

        //shutdown JDA
        log.info("Shutting down JDAs");
        jdas.forEach(JDA::shutdown);

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
