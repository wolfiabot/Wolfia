package de.npstr.wolfia.pregame;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Main;
import de.npstr.wolfia.Player;
import de.npstr.wolfia.pregame.commands.InCommand;
import de.npstr.wolfia.pregame.commands.OutCommand;
import de.npstr.wolfia.pregame.commands.SignUpStatusCommand;
import de.npstr.wolfia.pregame.commands.SingUpCommand;
import de.npstr.wolfia.utils.CommandParser;
import net.dv8tion.jda.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by npstr on 24.08.2016
 */
public class Pregame {

    private Map<String, Date> innedPlayers;
    private TextChannel channel;
    private static HashMap<String, Command> commands = new HashMap<>();
    private final static Logger LOG = LogManager.getLogger();


    public Pregame(TextChannel channel) {
        innedPlayers = new HashMap<>();
        this.channel = channel;

        commands.put("in", new InCommand(this));
        commands.put("out", new OutCommand(this));
        commands.put("signups", new SignUpStatusCommand(this));
        commands.put("singups", new SingUpCommand(this));

        Main.handleOutputMessage(channel, "Pregame now open");
        postSignUps();

        //this will keep track of players falling out of signups after a time
        Runnable checkSignupListEvery60Sec = () -> {
            try {
                while (true) {
                    TimeUnit.SECONDS.sleep(60);
                    LOG.trace("check signup list task executing");
                    clearSignUpList();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(checkSignupListEvery60Sec);
        thread.start();
    }

    public void inPlayer(String userId, int mins) {
        innedPlayers.put(userId, new Date(System.currentTimeMillis() + mins * 60 * 1000));
        postSignUps();
    }

    public void outPlayer(String userId) {
        innedPlayers.remove(userId);
        postSignUps();
    }

    public void postSignUps() {
        String output = "Current signups: " + innedPlayers.size() + " players\n";
        for (String userId : innedPlayers.keySet()) {
            Date till = innedPlayers.get(userId);

            long diff = till.getTime() - System.currentTimeMillis();
            diff /= 1000;
            long mins = diff / 60;
            long hours = mins / 60;
            mins = mins % 60;
            output += Player.getDiscordNick(userId) + " for " + hours + "h " + mins + "m\n";
        }

        Main.handleOutputMessage(channel, output);
    }

    // remove players that went beyond their sign up time
    private void clearSignUpList() {
        List<String> gtfo = new ArrayList<>();
        for (String userId : innedPlayers.keySet()) {
            if (innedPlayers.get(userId).before(new Date())) {
                gtfo.add(userId);
            }
        }

        for (String userId : gtfo) {
            innedPlayers.remove(userId);
            Main.handleOutputMessage(channel, Player.asMention(userId) + " removed from signups");
            Main.handleOutputMessage(userId, "removed you from the signup list");
        }
    }

    static void handleCommand(CommandParser.CommandContainer cmd) {
        if (commands.containsKey(cmd.invoke)) {
            Command c = commands.get(cmd.invoke);
            boolean safe = c.argumentsValid(cmd.args, cmd.event);

            if (safe) {
                c.execute(cmd.args, cmd.event);
                c.executed(safe, cmd.event);
            } else {
                c.executed(safe, cmd.event);
            }
        } else {
            //TODO unrecognizable command, tell the user about it?
        }
    }

    public TextChannel getChannel() {
        return channel;
    }
}