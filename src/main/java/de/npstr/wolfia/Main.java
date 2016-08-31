package de.npstr.wolfia;

/**
 * Created by npstr on 22.08.2016
 */

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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class Main extends ListenerAdapter {

    public enum LOG {TRACE, DEBUG, INFO, WARN, ERROR}


    private static JDA jda;
    public static final CommandParser parser = new CommandParser();
    private static HashMap<String, Command> commands = new HashMap<>();

    private final static Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

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
        //TODO check if the pregame channel is up at all
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

    public static void log(String msg) {
        log(LOG.DEBUG, msg);
    }

    public static void log(LOG level, String msg) {

        Level lvl = Level.DEBUG;
        switch (level) {
            case ERROR:
                lvl = Level.ERROR;
                break;
            case WARN:
                lvl = Level.WARN;
                break;
            case INFO:
                lvl = Level.INFO;
                break;
            case DEBUG:
                lvl = Level.DEBUG;
                break;
            case TRACE:
                lvl = Level.TRACE;
                break;
        }
        log.log(lvl, msg);
    }
}