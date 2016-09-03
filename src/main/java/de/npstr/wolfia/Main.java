package de.npstr.wolfia;

/**
 * Created by npstr on 22.08.2016
 */

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.api.sync.RedisCommands;
import de.npstr.wolfia.commands.PingCommand;
import de.npstr.wolfia.pregame.Pregame;
import de.npstr.wolfia.pregame.PregameListener;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.Sneaky;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class Main extends ListenerAdapter {

    public enum LOG {TRACE, DEBUG, INFO, WARN, ERROR}

    public static final CommandParser parser = new CommandParser();

    private static final String REDIS_URI = "redis://localhost:6379";
    private static JDA jda;
    private static HashMap<String, Command> commands = new HashMap<>();
    private final static Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {

        RedisCommands<String, String> redisSync = null;
        //connect to DB
        RedisClient redisClient = RedisClient.create(REDIS_URI);
        try {
            redisSync = redisClient.connect().sync();
            LOG.trace("established connection to DB redis @ " + REDIS_URI);
        } catch (RedisConnectionException e) {
            e.printStackTrace();
            LOG.error("could not establish connection to DB redis @ " + REDIS_URI + ", exiting");
            return;
        }


        //setting up JDA
        try {
            jda = new JDABuilder().addListener(new MainListener()).setBotToken(Sneaky.DISCORD_TOKEN).buildBlocking();
            jda.setAutoReconnect(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //adding commands
        commands.put("ping", new PingCommand());

        //start pregame
        for (Guild g : jda.getGuilds()) {
            TextChannel pregameChannel = null;
            for (TextChannel txt : g.getTextChannels())
                if (txt.getName().equals("pregame")) pregameChannel = txt;

            if (pregameChannel == null)
                pregameChannel = (TextChannel) g.createTextChannel("pregame").getChannel();

            Pregame pg = new Pregame(pregameChannel);
            jda.addEventListener(new PregameListener(pg));
        }
    }

    public static void handleCommand(CommandParser.CommandContainer cmd) {
        if (commands.containsKey(cmd.invoke)) {
            Command c = commands.get(cmd.invoke);
            boolean safe = c.called(cmd.args, cmd.event);

            if (safe) {
                c.action(cmd.args, cmd.event);
                c.executed(safe, cmd.event);
            } else {
                c.executed(safe, cmd.event);
            }
        } else {
            //TODO unrecognizable command, tell the user about it?
        }
    }


    public static void handleOutputMessage(MessageChannel channel, String msg) {
        //TODO fancy it up with color?
        channel.sendMessage(msg);
    }
}