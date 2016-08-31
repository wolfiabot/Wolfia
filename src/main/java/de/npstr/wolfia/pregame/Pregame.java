package de.npstr.wolfia.pregame;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Main;
import de.npstr.wolfia.game.Player;
import de.npstr.wolfia.pregame.commands.InCommand;
import de.npstr.wolfia.pregame.commands.OutCommand;
import de.npstr.wolfia.pregame.commands.SignUpStatusCommand;
import de.npstr.wolfia.utils.CommandParser;
import net.dv8tion.jda.entities.TextChannel;

import java.util.*;

/**
 * Created by npstr on 24.08.2016
 */
public class Pregame {

    private Map<Player, Date> innedPlayers;
    private TextChannel channel;
    private static HashMap<String, Command> commands = new HashMap<>();


    public Pregame(TextChannel channel) {
        innedPlayers = new HashMap<>();
        this.channel = channel;

        commands.put("in", new InCommand(this));
        commands.put("out", new OutCommand(this));
        commands.put("signups", new SignUpStatusCommand(this));

        Main.handleOutputMessage(channel, "Pregame now open");
        postSignUps();


    }

    public void inPlayer(Player p) {
        innedPlayers.put(p, new Date(System.currentTimeMillis() + 3600000));
        postSignUps();
    }

    public void outPlayer(Player p) {
        innedPlayers.remove(p);
        postSignUps();
    }

    public void postSignUps() {
        clearSignUpList();

        String out = "Current sign ups:\n";
        for (Player p : innedPlayers.keySet()) {
            Date till = innedPlayers.get(p);

            long diff = till.getTime() - System.currentTimeMillis();
            diff /= 1000;
            long mins = diff / 60;
            long hours = mins / 60;
            mins = mins % 60;
            out += p.getUser().getUsername() + " for " + hours + "h " + mins + "m\n";
        }

        Main.handleOutputMessage(channel, out);
    }

    // remove players that went beyond their sign up time
    private void clearSignUpList() {
        List<Player> gtfo = new ArrayList<>();
        for (Player p : innedPlayers.keySet()) {
            if (innedPlayers.get(p).before(new Date())) {
                gtfo.add(p);
            }
        }

        for (Player p : gtfo) {
            innedPlayers.remove(p);
            Main.handleOutputMessage(channel, p.getUser().getAsMention() + " removed from sign ups");
            Main.handleOutputMessage(p.getUser().getPrivateChannel(), "removed you from the sign up list");
        }
    }

    static void handleCommand(CommandParser.CommandContainer cmd) {
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

    public TextChannel getChannel() {
        return channel;
    }
}