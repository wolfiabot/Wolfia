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

/**
 * Created by npstr on 22.08.2016
 * <p>
 * Needed Permissions:
 * Reading messages in turbo chat (d'uh)
 * Writing Messages in turbo chat (d'uh)
 * Mentioning Users in turbo chat (d'uh)
 * <p>
 * Access to Message History to provide chatlogs
 * <p>
 * Manage RoleUtils permission to mute and unmute players/channels during ongoing games
 * <p>
 * <p>
 * Nice to have:
 * Creating TextChannels so the bot can create a turbo-chat if it is missing for whatever reason
 * Reading Messages server wide (for better keeping track of inactive players)
 */

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import com.github.napstr.logback.DiscordAppender;
import com.google.gson.Gson;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.SimpleLog;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.commands.meta.ICommand;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.log.JDASimpleLogListener;

import java.util.HashMap;
import java.util.function.Consumer;

public class Wolfia {

    public static JDA jda;
    public static final OkHttpClient httpClient = new OkHttpClient();
    public static final long START_TIME = System.currentTimeMillis();

    private final HashMap<String, ICommand> commands = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(Wolfia.class);

//    private static RedisClient redisClient;
//    private static DBWrapper mainDB;

    private static final Gson GSON = new Gson();
    private static final String DB_PREFIX = "wolfia:";
    private static final String DB_PREFIX_PLAYER = DB_PREFIX + "player:";
    private static final String DB_PREFIX_PREGAME = DB_PREFIX + "unhaim:";


    //set up things that are crucial
    //if something fails exit right away
    public static void main(final String[] args) {
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);

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

        //connect to DB & distribute db objects to classes
//        final RedisURI rUI = RedisURI.builder().withHost("localhost").withPort(6379).withPassword(Config.C.redisAuth).build();
//        redisClient = RedisClient.create(rUI);
//        final RedisCommands<String, String> redisSync;
//        try {
//            redisSync = redisClient.connect().sync();
//            mainDB = new DBWrapper(DB_PREFIX, redisSync, GSON);
//            //try writing and reading as a simple test
//            mainDB.set("key", "value");
//            mainDB.get("key", String.class);
//            mainDB.del("key");
//            Player.setDB(new DBWrapper(DB_PREFIX_PLAYER, redisSync, GSON));
//
//            log.info("Established connection to redis DB");
//        } catch (final RedisConnectionException e) {
//            log.error("could not establish connection to redis DB, exiting", e);
//            return;
//        } catch (final RedisCommandExecutionException e) {
//            log.error("could not execute commands on redis DB, possibly wrong AUTH, exiting", e);
//            return;
//        }


        new Wolfia();
    }

    private Wolfia() {
        //setting up JDA
        log.info("Setting up JDA and main listener");
        final MainListener mainListener = new MainListener();
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(Config.C.discordToken)
                    .addEventListener(mainListener)
                    .setEnableShutdownHook(false)
                    .buildBlocking();
        } catch (final Exception e) {
            log.error("could not create JDA object, possibly invalid bot token, exiting", e);
            return;
        }

    }

    public static void handleOutputMessage(final MessageChannel channel, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        final MessageBuilder mb = new MessageBuilder();
        mb.appendFormat(msg, args);
        try {
            channel.sendMessage(mb.build()).queue(null, onFail);
        } catch (final PermissionException e) {
            log.error("Could not post a message in channel {} due to missing permission {}", channel.getId(), e.getPermission().name(), e);
        }
    }

    public static void handleOutputMessage(final MessageChannel channel, final String msg, final Object... args) {
        handleOutputMessage(channel, null, msg, args);
    }

    public static void handleOutputMessage(final long channelId, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        final TextChannel channel = jda.getTextChannelById(channelId);
        handleOutputMessage(channel, onFail, msg, args);
    }

    public static void handleOutputMessage(final long channelId, final String msg, final Object... args) {
        handleOutputMessage(channelId, null, msg, args);
    }

    //send a message to a user privately
    public static void handlePrivateOutputMessage(final long userId, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        jda.getUserById(userId).openPrivateChannel().queue((privateChannel) -> Wolfia.handleOutputMessage(privateChannel, onFail, msg, args), onFail);
    }

    public static void shutdown(final int code) {
        log.info("Shutting down with exit code {}", code);
        System.exit(code);
    }

    private static final Thread SHUTDOWN_HOOK = new Thread(new Runnable() {
        @Override
        public void run() {

            //okHttpClient claims that a shutdown isn't necessary

            //shutdown JDA
            jda.shutdown(true);

            //shutdown DB
//            redisClient.shutdown();

            //shutdown logback logger
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();
        }
    });
}