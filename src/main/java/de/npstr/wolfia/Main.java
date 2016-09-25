package de.npstr.wolfia;

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
import de.npstr.wolfia.commands.ChatLogCommand;
import de.npstr.wolfia.pregame.Pregame;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.DBWrapper;
import de.npstr.wolfia.utils.Player;
import de.npstr.wolfia.utils.Sneaky;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.exceptions.PermissionException;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;

public class Main extends ListenerAdapter {

    public static JDA jda;


    private static Guild activeServer;

    private static HashMap<String, Command> commands = new HashMap<>();
    private static final Logger LOG = LogManager.getLogger();

    private static final String TURBO_CHAT_ROOM_NAME = "turbo-chat";

    private static DBWrapper mainDB;

    private static final Gson GSON = new Gson();
    private static final String DB_PREFIX = "wolfia:";
    private static final String DB_PREFIX_PLAYER = DB_PREFIX + "player:";
    private static final String DB_PREFIX_PREGAME = DB_PREFIX + "pregame:";

    public static void main(String[] args) {

        //connect to DB & distribute db objects to classes
        //kill itself if that doesn't work
        RedisURI rUI = RedisURI.builder().withHost("localhost").withPort(6379).withPassword(Sneaky.REDIS_AUTH()).build();
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

            LOG.trace("established connection to redis DB");
        } catch (RedisConnectionException e) {
            e.printStackTrace();
            LOG.error("could not establish connection to redis DB, exiting");
            return;
        } catch (RedisCommandExecutionException e) {
            e.printStackTrace();
            LOG.error("could not execute commands on redis DB, possibly wrong AUTH, exiting");
            return;
        }


        //setting up JDA
        //kill itself it if doesn't work
        MainListener mainListener = new MainListener();
        try {
            jda = new JDABuilder()
                    .addListener(mainListener)
                    .setBotToken(Sneaky.DISCORD_TOKEN())
                    .setBulkDeleteSplittingEnabled(false) //dont forget to handle bulk delete event
                    .buildBlocking();
            jda.setAutoReconnect(true);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("could not create JDA object, possibly invalid bot token, exiting");
            redisClient.shutdown();
            return;
        }

        //adding commands
        commands.put(ChatLogCommand.COMMAND, new ChatLogCommand(mainListener));

        //finding the guild aka discord server
        String serverId = Sneaky.DISCORD_SERVER_ID();
        if (args.length > 0) serverId = args[0];
        activeServer = jda.getGuildById(serverId);

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
            LOG.warn("did not find the turbo chat channel " + TURBO_CHAT_ROOM_NAME + ", attempting to create one");
            try {
                turboChannel = (TextChannel) activeServer.createTextChannel(TURBO_CHAT_ROOM_NAME).getChannel();
                mainDB.set("turbochatid", turboChannel.getId(), true);
            } catch (PermissionException e) {
                LOG.warn("could not create turbo-chat, missing permission to create text channels");
            }
        }
        if (turboChannel != null) {
            Pregame pg = new Pregame(turboChannel, new DBWrapper(DB_PREFIX_PREGAME + turboChannel.getId() + ":", redisSync, GSON), null);
            jda.addEventListener(pg.getListener());
        }
    }

    public static boolean hasChatLogAuth(User user) {
        List<Role> roles = activeServer.getRolesForUser(user);
        for (Role r : roles) {
            if (r.getName().equals("Staff")) return true;
        }

        return user.getId().equals(Sneaky.NPSTR_ID);
    }


    static void handleCommand(CommandParser.CommandContainer cmd) {
        if (commands.containsKey(cmd.invoke)) {
            LOG.trace("handling command " + cmd.invoke);
            Command c = commands.get(cmd.invoke);
            boolean safe = c.argumentsValid(cmd.args, cmd.event);

            if (safe) c.execute(cmd.args, cmd.event);
            c.executed(safe, cmd.event);
        }
    }

    public static void handleOutputMessage(MessageChannel channel, String msg) {
        channel.sendMessage(msg);
    }

    public static void handleOutputMessage(String userId, String msg) {
        PrivateChannel ch = jda.getUserById(userId).getPrivateChannel();
        handleOutputMessage(ch, msg);
    }
}