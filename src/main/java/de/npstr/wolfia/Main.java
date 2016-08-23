package de.npstr.wolfia;

/**
 * Created by npstr on 22.08.2016
 */

import de.npstr.wolfia.commands.PingCommand;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.Sneaky;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class Main extends ListenerAdapter {

    enum LOG {TRACE, DEBUG, INFO, WARN, ERROR}


    private static JDA jda;
    static final CommandParser parser = new CommandParser();
    private static HashMap<String, Command> commands = new HashMap<>();

    private final static Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        //setting up JDA
        try {
            jda = new JDABuilder().addListener(new BotListener()).setBotToken(Sneaky.DISCORD_TOKEN).buildBlocking();
            jda.setAutoReconnect(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //setting up logger


        commands.put("ping", new PingCommand());
    }

    public static void handleCommand(CommandParser.CommandContainer cmd) {
        if (commands.containsKey(cmd.invoke)) {
            boolean safe = commands.get(cmd.invoke).called(cmd.args, cmd.event);

            if (safe) {
                commands.get(cmd.invoke).action(cmd.args, cmd.event);
                commands.get(cmd.invoke).executed(safe, cmd.event);
            } else {
                commands.get(cmd.invoke).executed(safe, cmd.event);
            }
        }
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