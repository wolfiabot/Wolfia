package de.npstr.wolfia.pregame;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.CommandListener;
import de.npstr.wolfia.Game;
import de.npstr.wolfia.Main;
import de.npstr.wolfia.pregame.commands.*;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.DBWrapper;
import de.npstr.wolfia.utils.Player;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by npstr on 24.08.2016
 */
public class Pregame {

    private Set<String> innedPlayers;
    private final TextChannel channel;
    private static final Map<String, Command> commands = new HashMap<>();
    private final PregameListener listener;
    private final static Logger LOG = LogManager.getLogger();
    public static final String PREFIX = "!";


    private final DBWrapper pregameDB;

    @SuppressWarnings("unchecked")
    public Pregame(TextChannel channel, DBWrapper db, Game game) {
        this.channel = channel;
        this.pregameDB = db;
        this.listener = new PregameListener(this);
        innedPlayers = getInnedPlayers();

        commands.put(InCommand.COMMAND, new InCommand(listener, this));
        commands.put(OutCommand.COMMAND, new OutCommand(listener, this));
        commands.put(SignUpStatusCommand.COMMAND, new SignUpStatusCommand(listener, this));
        if (game != null) {
            //put more commands here
        }
        commands.put(HelpCommand.COMMAND, new HelpCommand(listener, new HashMap<>(commands)));
        commands.put(SingUpCommand.COMMAND, new SingUpCommand(listener));//put this at the end so it doesn't show up in the HELP command

        Main.handleOutputMessage(channel, "Bot started, signups now open in this channel");
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

    @SuppressWarnings("unchecked")
    private Set<String> getInnedPlayers() {
        innedPlayers = pregameDB.get("innedPlayers", Set.class);
        if (innedPlayers == null) innedPlayers = new HashSet<>();
        pregameDB.set("innedPlayers", innedPlayers);
        return innedPlayers;
    }

    @SuppressWarnings("unchecked")
    public void inPlayer(String userId, long mins) {
        innedPlayers = getInnedPlayers();
        innedPlayers.add(userId);
        pregameDB.set(userId, System.currentTimeMillis() + mins * 60 * 1000);
        pregameDB.set("innedPlayers", innedPlayers);
        postSignUps();
    }

    @SuppressWarnings("unchecked")
    public void outPlayer(String userId) {
        innedPlayers = getInnedPlayers();
        boolean wasInned = innedPlayers.remove(userId);
        pregameDB.del(userId);
        pregameDB.set("innedPlayers", innedPlayers);
        if (wasInned) {
            Main.handleOutputMessage(channel, Player.asMention(userId) + " outed.");
            postSignUps();
        } else {
            Main.handleOutputMessage(channel, Player.asMention(userId) + " you aren't even inned bro.");
        }

    }

    @SuppressWarnings("unchecked")
    public void postSignUps() {
        innedPlayers = getInnedPlayers();
        String output = "Current signups: " + innedPlayers.size() + " players\n";
        for (String userId : innedPlayers) {
            Long till = pregameDB.get(userId, Long.class);

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
    @SuppressWarnings("unchecked")
    private void clearSignUpList() {
        List<String> gtfo = new ArrayList<>();
        innedPlayers = getInnedPlayers();
        for (String userId : innedPlayers) {
            long till = pregameDB.get(userId, Long.class);
            if (till < System.currentTimeMillis()) {
                gtfo.add(userId);
                continue;
            }

            long lastSeen = Player.lastSeen(userId);
            Long lastWarning = pregameDB.get("warningSent:" + userId, Long.class);
            if (lastWarning == null) lastWarning = 0L;

            if ((System.currentTimeMillis() - lastSeen > 3600000) & // 1hour of inactivity
                    (System.currentTimeMillis() - lastWarning) > 600000) //10 mins since last warning
            {
                Main.handleOutputMessage(userId, "Heya " + Player.getDiscordNick(userId) + ", you signed up for a" +
                        "turbo, but I haven't seen you around for a while. You will be removed from the signup list " +
                        "in 5 minutes due to inactivity. To prevent that write me a message here or on the Mafia " +
                        "Universe Discord Server.");
                pregameDB.set("warningSent:" + userId, System.currentTimeMillis());
            }

            if (System.currentTimeMillis() - lastSeen > 3900000) // 1hour 5 mins of inactivity
            {
                gtfo.add(userId);
            }
        }

        for (String userId : gtfo) {
            innedPlayers.remove(userId);
            pregameDB.set("innedPlayers", innedPlayers);
            pregameDB.del(userId);
            Main.handleOutputMessage(channel, Player.asMention(userId) + " removed from signups");
            Main.handleOutputMessage(userId, "removed you from the signup list");
        }
        if (gtfo.size() > 0) {
            postSignUps();
        }
    }

    void handleCommand(CommandParser.CommandContainer cmd) {
        if (commands.containsKey(cmd.invoke)) {
            Command c = commands.get(cmd.invoke);
            boolean safe = c.argumentsValid(cmd.args, cmd.event);

            if (safe) c.execute(cmd.args, cmd.event);
            c.executed(safe, cmd.event);
        }
    }

    public TextChannel getChannel() {
        return channel;
    }

    public PregameListener getListener() {
        return listener;
    }
}

class PregameListener extends ListenerAdapter implements CommandListener {

    private final Pregame pregame;
    private final static Logger LOG = LogManager.getLogger();


    PregameListener(Pregame pg) {
        super();
        pregame = pg;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) {
            return;
        }

        //is this our channel?
        if (!pregame.getChannel().getId().equals(event.getTextChannel().getId())) {
            return;
        }

        //does the message have our prefix?
        if (event.getMessage().getContent().startsWith(Pregame.PREFIX)) {
            pregame.handleCommand(CommandParser.parse(Pregame.PREFIX, event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOG.trace("PregameListener on channel " + pregame.getChannel().getName() + " started");
    }

    @Override
    public String getPrefix() {
        return Pregame.PREFIX;
    }
}
