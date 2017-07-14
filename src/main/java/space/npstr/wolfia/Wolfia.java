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
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.SimpleLog;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.charts.Charts;
import space.npstr.wolfia.db.DbManager;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.Hstore;
import space.npstr.wolfia.db.entity.PrivateGuild;
import space.npstr.wolfia.db.entity.stats.GeneralBotStats;
import space.npstr.wolfia.db.entity.stats.MessageOutputStats;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.ImgurAlbum;
import space.npstr.wolfia.utils.SimpleCache;
import space.npstr.wolfia.utils.log.JDASimpleLogListener;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by npstr on 22.08.2016
 * <p>
 * Main class of Wolfia
 */
public class Wolfia {

    public static JDA jda;
    public static DbManager dbManager;
    public static OkHttpClient httpClient = new OkHttpClient();
    public static final long START_TIME = System.currentTimeMillis();
    public static final LinkedBlockingQueue<PrivateGuild> AVAILABLE_PRIVATE_GUILD_QUEUE = new LinkedBlockingQueue<>();
    //for any fire and forget tasks that are expected to run for a short while only
    public static final ExecutorService executor = Executors.newCachedThreadPool();
    //true if a restart is planned, or live maintenance is happening, so games wont be able to be started
    public static Wolfia wolfia;

    //default on fail handler for all queues()
    public static Consumer<Throwable> defaultOnFail;

    private static final Logger log = LoggerFactory.getLogger(Wolfia.class);
    //for long running, repeating and/or scheduled tasks
    private static final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    private static final List<ScheduledFuture> runningTasks = new ArrayList<>();

    private static final ImgurAlbum avatars = new ImgurAlbum(Config.C.avatars);

    public final CommandListener commandListener;

    //set up things that are crucial
    //if something fails exit right away
    public static void main(final String[] args) {
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);
        defaultOnFail = t -> log.error("Exception during queue(): {}", t.getMessage(), t);

        //reroute JDA logging to our system
        SimpleLog.LEVEL = SimpleLog.Level.OFF;
        SimpleLog.addListener(new JDASimpleLogListener());

        log.info("Starting Wolfia v" + App.VERSION);

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
        dbManager = new DbManager();

        AVAILABLE_PRIVATE_GUILD_QUEUE.addAll(DbWrapper.loadPrivateGuilds());
        log.info("{} private guilds loaded", AVAILABLE_PRIVATE_GUILD_QUEUE.size());

        //start the bot
        wolfia = new Wolfia();

        //fire up spark
        Charts.spark();

        //post stats every 10 minutes
        scheduleAtFixedRate(Wolfia::postBotStats, 1, 10, TimeUnit.MINUTES);

        //set up a random avatar and change every 2 hours
        final Hstore defaultHstore = Hstore.load();
        final int lastIndex = Integer.valueOf(defaultHstore.get("avatarLastIndex", "-1"));
        avatars.setLastIndex(lastIndex);
        avatars.get(lastIndex);

        final long lastUpdated = Long.valueOf(defaultHstore.get("avatarLastUpdated", "0"));
        final long initialDelay = lastUpdated - System.currentTimeMillis() + TimeUnit.HOURS.toMillis(2);
        log.info("Updating avatar in {}ms", initialDelay);

        scheduleAtFixedRate(() -> {
            try {
                setAvatars(Icon.from(SimpleCache.getImageFromURL(avatars.getNext())));
                Hstore.loadAndSet("avatarLastIndex", Integer.toString(avatars.getLastIndex()))
                        .set("avatarLastUpdated", Long.toString(System.currentTimeMillis()))
                        .save();
                log.info("Avatar updated");
            } catch (final IOException e) {
                log.error("Could not set avatar.", e);
            }
        }, initialDelay, TimeUnit.HOURS.toMillis(2), TimeUnit.MILLISECONDS);
    }

    public static void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit timeUnit) {
        runningTasks.add(scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, timeUnit));
    }

    public static void schedule(final Runnable task, final long delay, final TimeUnit timeUnit) {
        runningTasks.add(scheduledExecutor.schedule(task, delay, timeUnit));
    }


    private static void postBotStats() {
        if (jda == null) {
            log.warn("Skipping posting of bot stats due to JDA being null");
            return;
        }

        DbWrapper.persist(new GeneralBotStats(
                jda.getUsers().size(),
                jda.getGuilds().size(),
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

    private Wolfia() {
        //setting up JDA
        log.info("Setting up JDA and main listener");
        this.commandListener = new CommandListener(
                AVAILABLE_PRIVATE_GUILD_QUEUE.stream().map(PrivateGuild::getId).collect(Collectors.toList()));
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(Config.C.discordToken)
                    .addEventListener(this.commandListener)
                    .addEventListener(AVAILABLE_PRIVATE_GUILD_QUEUE.toArray())
                    .setEnableShutdownHook(false)
                    .setGame(Game.of(App.GAME_STATUS))
                    .buildBlocking();
        } catch (final Exception e) {
            log.error("could not create JDA object, possibly invalid bot token, exiting", e);
            return;
        }

        jda.asBot().getApplicationInfo().queue(
                appInfo -> {
                    App.OWNER_ID = appInfo.getOwner().getIdLong();
                    App.INVITE_LINK = appInfo.getInviteUrl(0);
                    App.DESCRIPTION = appInfo.getDescription();
                },
                t -> log.error("Could not load application info", t));
    }

    //set avatar for bot and wolfia lounge
    private static void setAvatars(final Icon icon) {
        final Guild wolfiaLounge = jda.getGuildById(App.WOLFIA_LOUNGE_ID);
        jda.getSelfUser().getManager().setAvatar(icon).queue(null, defaultOnFail);
        if (!Config.C.isDebug && wolfiaLounge != null && wolfiaLounge.getSelfMember().hasPermission(Permission.MANAGE_SERVER)) {
            wolfiaLounge.getManager().setIcon(icon).queue(null, defaultOnFail);
        }
    }

    //################## message handling + tons of overloaded methods

    //calling with complete = true will ignore onsuccess and on fail, but return an optional with the message
    private static Optional<Message> handleOutputMessage(final boolean complete, final MessageChannel channel, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        if (complete && (onSuccess != null || onFail != null)) {
            log.warn("called handleOutputMessage() with complete set to true AND an onSuccess or onFail handler. check your code, dude");
        }
        final MessageBuilder mb = new MessageBuilder();
        mb.appendFormat(msg, args);
        try {
            final RestAction<Message> ra = channel.sendMessage(mb.build());
            if (complete) {
                final Message message = ra.complete();
                executor.submit(() -> DbWrapper.persist(new MessageOutputStats(message)));
                return Optional.of(message);
            } else {
                Consumer<Throwable> fail = onFail;
                if (fail == null) {
                    fail = throwable -> {
                        if (!(channel instanceof PrivateChannel)) //ignore exceptions when sending to private channels
                            log.error("Exception when sending a message in channel {}", channel.getIdLong(), throwable);
                    };
                }
                //for stats keeping
                final Consumer<Message> wrappedSuccess = (message) -> {
                    executor.submit(() -> DbWrapper.persist(new MessageOutputStats(message)));
                    if (onSuccess != null) onSuccess.accept(message);
                };
                ra.queue(wrappedSuccess, fail);
            }
        } catch (final PermissionException e) {
            log.error("Could not post a message in channel {} due to missing permission {}", channel.getId(), e.getPermission().name(), e);
        }
        return Optional.empty();
    }

    public static Optional<Message> handleOutputMessage(final boolean complete, final MessageChannel channel, final String msg, final Object... args) {
        return handleOutputMessage(complete, channel, null, null, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final boolean complete, final long channelId, final String msg, final Object... args) {
        final TextChannel channel = jda.getTextChannelById(channelId);
        return handleOutputMessage(complete, channel, null, null, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final MessageChannel channel, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        return handleOutputMessage(false, channel, onSuccess, onFail, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final long channelId, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        final TextChannel channel = jda.getTextChannelById(channelId);
        return handleOutputMessage(channel, onSuccess, onFail, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final MessageChannel channel, final String msg, final Object... args) {
        return handleOutputMessage(false, channel, null, null, msg, args);
    }

    public static Optional<Message> handleOutputMessage(final long channelId, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        final TextChannel channel = jda.getTextChannelById(channelId);
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
            if (!tc.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_EMBED_LINKS)) {
                handleOutputMessage(channel, "Hey, I am missing the `%s` permission to display my messages properly in this channel.", Permission.MESSAGE_EMBED_LINKS.getName());
                return Optional.empty();
            }
        }
        try {
            final RestAction<Message> ra = channel.sendMessage(msgEmbed);
            if (complete) {
                final Message message = ra.complete();
                executor.submit(() -> DbWrapper.persist(new MessageOutputStats(message)));
                return Optional.of(message);
            } else {
                //for stats keeping
                final Consumer<Message> wrappedSuccess = (message) -> {
                    executor.submit(() -> DbWrapper.persist(new MessageOutputStats(message)));
                    if (onSuccess != null) onSuccess.accept(message);
                };
                ra.queue(wrappedSuccess, onFail != null ? onFail : defaultOnFail);
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
        final TextChannel channel = jda.getTextChannelById(channelId);
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
        jda.getUserById(userId).openPrivateChannel().queue((privateChannel) -> Wolfia.handleOutputMessage(privateChannel, null, onFail, msg, args), onFail);
    }

    public static void handlePrivateOutputMessage(final long userId, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        if (onFail == null) {
            log.error("Trying to send a private message without an onFail handler :smh:. This may lead to unnecessary " +
                    "error log spam. Fix your code please.");
        }
        jda.getUserById(userId).openPrivateChannel().queue((privateChannel) -> Wolfia.handleOutputMessage(privateChannel, onSuccess, onFail, msg, args), onFail);
    }

    //################# end of message handling

    //################# shutdown handling

    public static void shutdown(final int code) {
        log.info("Shutting down with exit code {}", code);
        System.exit(code);
    }

    private static final Thread SHUTDOWN_HOOK = new Thread(new Runnable() {
        @Override
        public void run() {

            //okHttpClient claims that a shutdown isn't necessary

            //shutdown JDA
            jda.shutdown();

            //shutdown executors
            executor.shutdown();
            scheduledExecutor.shutdown();
            runningTasks.forEach(scheduledFuture -> scheduledFuture.cancel(true));
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                log.warn("Executor did not finish it's tasks after 30 seconds");
            }

            //shutdown DB
            dbManager.shutdown();

            //shutdown logback logger
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();
        }
    });
}