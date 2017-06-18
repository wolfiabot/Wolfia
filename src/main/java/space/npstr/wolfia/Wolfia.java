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
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.SimpleLog;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.db.DbManager;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.log.JDASimpleLogListener;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;

public class Wolfia {

    public static JDA jda;
    public static DbManager dbManager;
    public static final OkHttpClient httpClient = new OkHttpClient();
    public static final long START_TIME = System.currentTimeMillis();

    public final static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    //todo find a better place for this
    //true if a restart is planned, so games wont be able to be started
    public static boolean restartFlag = false;

    private static final Logger log = LoggerFactory.getLogger(Wolfia.class);

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

        //set up relational database
        dbManager = new DbManager();

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

    //################## message handling + tons of overloaded methods

    //calling with complete = true will ignore onsuccess and on fail
    private static void handleOutputMessage(final boolean complete, final MessageChannel channel, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        if (complete && (onSuccess != null || onFail != null)) {
            log.warn("called handleOutputMessage() with complete set to true AND an onSuccess or onFail handler. check your code, dude");
        }
        final MessageBuilder mb = new MessageBuilder();
        mb.appendFormat(msg, args);
        try {
            final RestAction<Message> ra = channel.sendMessage(mb.build());
            if (complete) {
                ra.complete();
            } else {
                ra.queue(onSuccess, onFail);
            }
        } catch (final PermissionException e) {
            log.error("Could not post a message in channel {} due to missing permission {}", channel.getId(), e.getPermission().name(), e);
        }
    }

    public static void handleOutputMessage(final boolean complete, final MessageChannel channel, final String msg, final Object... args) {
        handleOutputMessage(complete, channel, null, null, msg, args);
    }

    public static void handleOutputMessage(final boolean complete, final long channelId, final String msg, final Object... args) {
        final TextChannel channel = jda.getTextChannelById(channelId);
        handleOutputMessage(complete, channel, null, null, msg, args);
    }

    private static void handleOutputMessage(final MessageChannel channel, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        handleOutputMessage(false, channel, onSuccess, onFail, msg, args);
    }

    public static void handleOutputMessage(final MessageChannel channel, final String msg, final Object... args) {
        handleOutputMessage(false, channel, null, null, msg, args);
    }

    public static void handleOutputMessage(final long channelId, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        final TextChannel channel = jda.getTextChannelById(channelId);
        handleOutputMessage(false, channel, null, onFail, msg, args);
    }

    public static void handleOutputMessage(final long channelId, final String msg, final Object... args) {
        handleOutputMessage(channelId, null, msg, args);
    }

    //embeds
    public static void handleOutputEmbed(final MessageChannel channel, final MessageEmbed msgEmbed) {
        //check for embed permissions in a guild text channel
        if (channel instanceof TextChannel) {
            final TextChannel tc = (TextChannel) channel;
            if (!tc.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_EMBED_LINKS)) {
                handleOutputMessage(channel, "Hey, I am missing the **Embed Links** permission to display my messages properly in this channel.");
                return;
            }
        }

        try {
            channel.sendMessage(msgEmbed).queue();
        } catch (final PermissionException e) {
            log.error("Could not post a message in channel {} due to missing permission {}", channel.getId(), e.getPermission().name(), e);
        }
    }

    public static void handleOutputEmbed(final long channelId, final MessageEmbed msgEmbed) {
        final TextChannel channel = jda.getTextChannelById(channelId);
        handleOutputEmbed(channel, msgEmbed);
    }


    //send a message to a user privately
    public static void handlePrivateOutputMessage(final long userId, final Consumer<Throwable> onFail, final String msg, final Object... args) {
        jda.getUserById(userId).openPrivateChannel().queue((privateChannel) -> Wolfia.handleOutputMessage(privateChannel, null, onFail, msg, args), onFail);
    }

    public static void handlePrivateOutputMessage(final long userId, final Consumer<Message> onSuccess, final Consumer<Throwable> onFail, final String msg, final Object... args) {
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
            jda.shutdown(true);

            //shutdown DB
//            redisClient.shutdown();

            //shutdown logback logger
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();
        }
    });
}