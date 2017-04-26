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
 * Manage Roles permission to mute and unmute players/channels during ongoing games
 * <p>
 * <p>
 * Nice to have:
 * Creating TextChannels so the bot can create a turbo-chat if it is missing for whatever reason
 * Reading Messages server wide (for better keeping track of inactive players)
 */

import com.google.gson.Gson;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.sync.RedisCommands;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.PopcornGame.Popcorn;
import space.npstr.wolfia.pregame.Pregame;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.CommandParser;
import space.npstr.wolfia.utils.DBWrapper;
import space.npstr.wolfia.utils.Player;

import java.util.HashMap;

public class Main implements CommandHandler {

    public static JDA jda;

    private static Guild activeServer;

    private HashMap<String, Command> commands = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private final String TURBO_CHAT_ROOM_NAME = "turbo-chat";
    private final String POPCORN_CHAT_ROOM_NAME = "popcorn-chat";

    private DBWrapper mainDB;

    private final Gson GSON = new Gson();
    private final String DB_PREFIX = "wolfia:";
    private final String DB_PREFIX_PLAYER = DB_PREFIX + "player:";
    private final String DB_PREFIX_PREGAME = DB_PREFIX + "pregame:";

    private Main(String[] args) {

        log.info("Starting Wolfia v" + App.VERSION);


        if (Config.C.isDebug)

            log.info("Running DEBUG configuration");

        else

            log.info("Running PRODUCTION configuration");


        //connect to DB & distribute db objects to classes
        //kill itself if that doesn't work
        RedisURI rUI = RedisURI.builder().withHost("localhost").withPort(6379).withPassword(Config.C.redisAuth).build();
        RedisClient redisClient = RedisClient.create(rUI);
        RedisCommands<String, String> redisSync;
        try {
            redisSync = redisClient.connect().sync();
            mainDB = new DBWrapper(DB_PREFIX, redisSync, GSON);
            //try writing and reading as a simple test
            mainDB.set("key", "value");
            mainDB.get("key", String.class);
            mainDB.del("key");
            Player.setDB(new DBWrapper(DB_PREFIX_PLAYER, redisSync, GSON));

            log.trace("established connection to redis DB");
        } catch (RedisConnectionException e) {
            e.printStackTrace();
            log.error("could not establish connection to redis DB, exiting");
            return;
        } catch (RedisCommandExecutionException e) {
            e.printStackTrace();
            log.error("could not execute commands on redis DB, possibly wrong AUTH, exiting");
            return;
        }


        //setting up JDA
        //kill itself it if doesn't work
        MainListener mainListener = new MainListener(this);
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .addEventListener(mainListener)
                    .setToken(Config.C.discordToken)
                    .setBulkDeleteSplittingEnabled(false) //don't forget to handle bulk delete event
                    .buildBlocking();
            jda.setAutoReconnect(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("could not create JDA object, possibly invalid bot token, exiting");
            redisClient.shutdown();
            return;
        }


        //TODO remove this server scope crap
        activeServer = jda.getGuildById(214539058028740609L); //secrit debag servur

        //start pregame in #turbo-chat
        String asd = mainDB.get("turbochatid", String.class, true);
        TextChannel turboChannel = jda.getTextChannelById(asd);// try looking for the last channel we used
        if (turboChannel == null) mainDB.del("turbochatid"); // clear db of non-existent channel id

        if (asd == null || turboChannel == null) { //didn't find a channel, look for it by the default channel name
            for (TextChannel txt : activeServer.getTextChannels())
                if (txt.getName().equals(TURBO_CHAT_ROOM_NAME)) {
                    turboChannel = txt;
                    mainDB.set("turbochatid", turboChannel.getId(), true);
                }
        }
        if (turboChannel == null) {//didn't even find a channel by default name, log a warning & try creating one
            log.warn("did not find the turbo chat channel " + TURBO_CHAT_ROOM_NAME + ", attempting to create one");
            try {
                turboChannel = (TextChannel) activeServer.getController().createTextChannel(TURBO_CHAT_ROOM_NAME);
                mainDB.set("turbochatid", turboChannel.getId(), true);
            } catch (PermissionException e) {
                log.warn("could not create turbo-chat, missing permission to create text channels");
            }
        }
        if (turboChannel != null) {
            Pregame pg = new Pregame(turboChannel, new DBWrapper(DB_PREFIX_PREGAME + turboChannel.getId() + ":", redisSync, GSON), null);
            jda.addEventListener(pg.getListener());
        }

        //start pregame in #popcorn-chat
        String popcornChatId = mainDB.get("popcornchatid", String.class, true);
        TextChannel popcornChannel = jda.getTextChannelById(popcornChatId);// try looking for the last channel we used
        if (popcornChannel == null) mainDB.del("popcornchatid"); // clear db of non-existent channel id

        if (popcornChatId == null || popcornChannel == null) { //didn't find a channel, look for it by the default channel name
            for (TextChannel txt : activeServer.getTextChannels())
                if (txt.getName().equals(POPCORN_CHAT_ROOM_NAME)) {
                    popcornChannel = txt;
                    mainDB.set("popcornchatid", popcornChannel.getId(), true);
                }
        }
        if (popcornChannel == null) {//didn't even find a channel by default name, log a warning & try creating one
            log.warn("did not find the chat channel " + POPCORN_CHAT_ROOM_NAME + ", attempting to create one");
            try {
                popcornChannel = (TextChannel) activeServer.getController().createTextChannel(POPCORN_CHAT_ROOM_NAME);
                mainDB.set("popcornchatid", popcornChannel.getId(), true);
            } catch (PermissionException e) {
                log.warn("could not create chat channel " + POPCORN_CHAT_ROOM_NAME + ", missing permission to create text channels");
            }
        }
        if (popcornChannel != null) {
            Pregame pg = new Pregame(popcornChannel, new DBWrapper(DB_PREFIX_PREGAME + popcornChannel.getId() + ":", redisSync, GSON), new Popcorn(popcornChannel));
            jda.addEventListener(pg.getListener());
        }
    }

    public static void main(String[] args) {
        new Main(args);
    }


    @Override
    public void handleCommand(CommandParser.CommandContainer cmd) {
        if (commands.containsKey(cmd.invoke)) {
            log.trace("handling command " + cmd.invoke);
            Command c = commands.get(cmd.invoke);
            boolean safe = c.argumentsValid(cmd.args, cmd.event);

            if (safe) c.execute(cmd.args, cmd.event);
            c.executed(safe, cmd.event);
        }
    }

    public static void handleOutputMessage(MessageChannel channel, String msg) {
        try {
            channel.sendMessage(msg);
        } catch (PermissionException e) {
            log.warn("Could not post a message in channel " + channel.getId() + "due to missing permission " + e.getPermission().name());
            e.printStackTrace();
        }
    }

    public static void handleOutputMessage(String userId, String msg) {
        PrivateChannel ch = jda.getUserById(userId).getPrivateChannel();
        handleOutputMessage(ch, msg);
    }
}