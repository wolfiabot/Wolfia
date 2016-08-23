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

import java.util.HashMap;

public class Wolfia extends ListenerAdapter {

    private static JDA jda;
    private static final CommandParser parser = new CommandParser();
    private static HashMap<String, Command> commands = new HashMap<>();

    public static void main(String[] args) {

        try {

            jda = new JDABuilder().addListener(new BotListener()).setBotToken(Sneaky.DISCORD_TOKEN).buildBlocking();
            jda.setAutoReconnect(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

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
}