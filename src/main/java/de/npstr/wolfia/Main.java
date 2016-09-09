package de.npstr.wolfia;

/**
 * Created by npstr on 22.08.2016
 */

import com.google.gson.Gson;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.api.sync.RedisCommands;
import de.npstr.wolfia.commands.PingCommand;
import de.npstr.wolfia.pregame.Pregame;
import de.npstr.wolfia.pregame.PregameListener;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.DBWrapper;
import de.npstr.wolfia.utils.Sneaky;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class Main extends ListenerAdapter {

    public static final CommandParser parser = new CommandParser();
    public static JDA jda;
    public static Guild activeServer;

    private static HashMap<String, Command> commands = new HashMap<>();
    private static final Logger LOG = LogManager.getLogger();

    private static final String PREGAME_ROOM_NAME = "turbo-chat";

    private static final String REDIS_URI = "redis://localhost:6379";
    private static DBWrapper db;

    private static final Gson GSON = new Gson();
    private static final String DB_PREFIX = "wolfia:";
    private static final String DB_PREFIX_PLAYER = DB_PREFIX + "player:";
    private static final String DB_PREFIX_PREGAME = DB_PREFIX + "pregame:";

    public static void main(String[] args) {

        //connect to DB & distribute db objects to classes
        //kill itself if that doesn't work
        RedisClient redisClient = RedisClient.create(REDIS_URI);
        RedisCommands<String, String> redisSync;
        try {
            redisSync = redisClient.connect().sync();
            db = new DBWrapper(DB_PREFIX, redisSync, GSON);
            //try writing and reading as a simple test
            db.set("key", "value");
            db.get("key", String.class);
            db.del("key");
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
        MainListener mainListener = new MainListener();
        try {
            jda = new JDABuilder().addListener(mainListener).setBotToken(Sneaky.DISCORD_TOKEN()).buildBlocking();
            jda.setAutoReconnect(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //finding the guild aka discord server
        String serverId = Sneaky.DISCORD_SERVER_ID();
        if (args.length > 0) serverId = args[0];
        activeServer = jda.getGuildById(serverId);

        //adding commands
        commands.put("ping", new PingCommand());

        //start pregame
        Guild g = activeServer;
        TextChannel pregameChannel = null;
        for (TextChannel txt : g.getTextChannels())
            if (txt.getName().equals(PREGAME_ROOM_NAME)) pregameChannel = txt;

        if (pregameChannel == null)
            pregameChannel = (TextChannel) g.createTextChannel(PREGAME_ROOM_NAME).getChannel();

        Pregame pg = new Pregame(pregameChannel, new DBWrapper(DB_PREFIX_PREGAME + pregameChannel.getId() + ":", redisSync, GSON));
        mainListener.setListener(new PregameListener(pg), pregameChannel);
    }

//    public static void handleCommand(CommandParser.CommandContainer cmd) {
//        if (commands.containsKey(cmd.invoke)) {
//            Command c = commands.get(cmd.invoke);
//            boolean safe = c.argumentsValid(cmd.args, cmd.event);
//
//            if (safe) {
//                c.execute(cmd.args, cmd.event);
//                c.executed(safe, cmd.event);
//            } else {
//                c.executed(safe, cmd.event);
//            }
//        } else {
//            //TODO unrecognizable command, tell the user about it?
//        }
//    }

    public static void handleOutputMessage(MessageChannel channel, String msg) {
        channel.sendMessage(msg);
    }

    public static void handleOutputMessage(String userId, String msg) {
        PrivateChannel ch = jda.getUserById(userId).getPrivateChannel();
        handleOutputMessage(ch, msg);
    }
}