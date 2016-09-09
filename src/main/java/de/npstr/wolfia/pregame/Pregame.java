package de.npstr.wolfia.pregame;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Main;
import de.npstr.wolfia.Player;
import de.npstr.wolfia.pregame.commands.*;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.DBWrapper;
import net.dv8tion.jda.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by npstr on 24.08.2016
 */
public class Pregame {

    private Set<String> innedPlayers;
    private TextChannel channel;
    private static Map<String, Command> commands = new HashMap<>();
    private final static Logger LOG = LogManager.getLogger();

    private DBWrapper db;

    public Pregame(TextChannel channel, DBWrapper db) {
        innedPlayers = db.get("innedPlayers", Set.class);
        if (innedPlayers == null) innedPlayers = new HashSet<>();
        db.set("innedPlayers", innedPlayers);

        this.channel = channel;
        this.db = db;

        commands.put("in", new InCommand(this));
        commands.put("out", new OutCommand(this));
        commands.put("signups", new SignUpStatusCommand(this));
        commands.put("singups", new SingUpCommand(this));
        commands.put(HelpCommand.COMMAND, new HelpCommand(commands));

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

    public void inPlayer(String userId, long mins) {
        innedPlayers = db.get("innedPlayers", Set.class);
        innedPlayers.add(userId);
        db.set(userId, System.currentTimeMillis() + mins * 60 * 1000);
        db.set("innedPlayers", innedPlayers);
        postSignUps();
    }

    public void outPlayer(String userId) {
        innedPlayers = db.get("innedPlayers", Set.class);
        innedPlayers.remove(userId);
        db.del(userId);
        db.set("innedPlayers", innedPlayers);
        postSignUps();
    }

    public void postSignUps() {
        innedPlayers = db.get("innedPlayers", Set.class);
        String output = "Current signups: " + innedPlayers.size() + " players\n";
        for (String userId : innedPlayers) {
            Long till = db.get(userId, Long.class);

            long diff = till - System.currentTimeMillis();
            diff /= 1000;
            long mins = diff / 60;
            long hours = mins / 60;
            mins = mins % 60;
            output += "   " + Player.getDiscordNick(userId) + " for " + hours + "h " + mins + "m\n";
        }

        Main.handleOutputMessage(channel, output);
    }

    // remove players that went beyond their sign up time
    private void clearSignUpList() {
        List<String> gtfo = new ArrayList<>();
        innedPlayers = db.get("innedPlayers", Set.class);
        for (String userId : innedPlayers) {
            long till = db.get(userId, Long.class);
            if (till < System.currentTimeMillis()) {
                gtfo.add(userId);
            }
        }

        for (String userId : gtfo) {
            innedPlayers.remove(userId);
            db.set("innedPlayers", innedPlayers);
            db.del(userId);
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