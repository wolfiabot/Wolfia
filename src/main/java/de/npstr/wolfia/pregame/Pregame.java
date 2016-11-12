package de.npstr.wolfia.pregame;

import de.npstr.wolfia.*;
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
public class Pregame implements CommandHandler {

    private Set<String> innedPlayers;
    private Set<String> confirmedPlayers;
    private final TextChannel channel;
    private static final Map<String, Command> commands = new HashMap<>();
    private final PregameListener listener;
    private final static Logger LOG = LogManager.getLogger();
    public static final String PREFIX = "!";

    private Game game = null;
    private boolean gameGoing = false;
    private boolean confirmationsGoing = false;

    private final DBWrapper pregameDB;

    @SuppressWarnings("unchecked")
    public Pregame(TextChannel channel, DBWrapper db, Game game) {
        this.channel = channel;
        this.pregameDB = db;
        this.game = game;
        this.listener = new PregameListener(this);
        innedPlayers = getInnedPlayers();

        commands.put(InCommand.COMMAND, new InCommand(listener, this));
        commands.put(OutCommand.COMMAND, new OutCommand(listener, this));
        commands.put(SignUpStatusCommand.COMMAND, new SignUpStatusCommand(listener, this));
        if (game != null) {
            //put more commands here
            commands.put(StartCommand.COMMAND, new StartCommand(listener, this));
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
                    if (gameGoing || confirmationsGoing) continue; //dont need to do this when the thread is busy
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

    /**
     * should get called whenever a user attempts to start a game
     *
     * @return whether or not confirmations have been started, which will happen if there is a game that is played in
     * this channel, and there are enough players signed up for that game
     */
    public boolean startGame() {
        if (game == null) {
            return false;
        }
        if (!game.enoughPlayers(getInnedPlayers().size())) {
            return false;
        }

        startConfirmations();

        return true;
    }

    /**
     * Starts the confirmations phase, where there is a time frame during which the signed up players have to confirm
     * their presence so the game can start with no afks. Other players may hero in, but it should be considered bad
     * manners to do so before the original sign ups had enough time to confirm. Community needs to sort this out.
     */
    private void startConfirmations() {
        confirmedPlayers = new HashSet<>();
        commands.put(ConfirmCommand.COMMAND, new ConfirmCommand(listener, this));
        String mentions = "";
        for (String userId : getInnedPlayers()) {
            mentions += Player.asMention(userId) + " ";
            Main.handleOutputMessage(userId, "Hey " + Player.getDiscordNick(userId) + "a game you signed up for is " +
                    "about to start in " + channel.getAsMention() + ", please post " + PREFIX + ConfirmCommand.COMMAND +
                    " in there during the next 10 minutes.");
        }
        Main.handleOutputMessage(channel, "Enough players have signed up! Game start initiated. You have 10 minutes " +
                "to confirm that you are in, just type " + PREFIX + ConfirmCommand.COMMAND + ". Game starts as soon " +
                "as enough players confirmed.\n" + mentions);

        //this will hitler around every minute mentioning players we are waiting on
        Runnable hitlerConfirmations = () -> {
            int times = 9;
            try {
                while (times > 0) {
                    if (gameGoing) break; //if the game started we dont need this
                    times--;
                    TimeUnit.SECONDS.sleep(60);
                    LOG.trace("hitlering confirmations");
                    String out = "Oi! ";
                    for (String s : confirmedPlayers) out += Player.getDiscordNick(s) + " ";
                    out += " have confirmed!\n";
                    out += "Need " + (game.getAmountOfPlayers() - confirmedPlayers.size()) + " more players.\n";
                    for (String s : getInnedPlayers())
                        if (!confirmedPlayers.contains(s)) out += Player.asMention(s) + " ";

                    Main.handleOutputMessage(channel, out);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(hitlerConfirmations);
        thread.start();

        //this will check if confirmations worked out
        //possible bug if the game is over in less than 10 mins, then this thread will think the game never started
        Runnable checkConfirmations = () -> {
            try {
                TimeUnit.SECONDS.sleep(600);
                LOG.trace("checking confirmations");
                confirmationsGoing = false;
                if (gameGoing) return;
                else {
                    String out = "Game start aborted. Shame on these players that signed up but did not confirm:\n";
                    for (String s : getInnedPlayers())
                        if (!confirmedPlayers.contains(s)) out += Player.asMention(s) + " ";
                    Main.handleOutputMessage(channel, out);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread2 = new Thread(checkConfirmations);
        thread2.start();
    }

    /**
     * Call this to have a user confirm his participation for an upcoming game
     *
     * @param userId discord id of the user that is confirming
     */
    public void confirm(String userId) {
        if (!confirmedPlayers.contains(userId)) {
            confirmedPlayers.add(userId);
            if (confirmedPlayers.size() == game.getAmountOfPlayers()) {
                //TODO open the channel after a game is over
                game.start(confirmedPlayers);
                gameGoing = true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getInnedPlayers() {
        innedPlayers = pregameDB.get("innedPlayers", Set.class);
        if (innedPlayers == null) innedPlayers = new HashSet<>();
        pregameDB.set("innedPlayers", innedPlayers);
        return innedPlayers;
    }

    /**
     * Call this to put a user on the signup list
     *
     * @param userId discord id of the user signing up
     * @param mins   amount of minutes the user is signing up for
     */
    @SuppressWarnings("unchecked")
    public void inPlayer(String userId, long mins) {
        innedPlayers = getInnedPlayers();
        innedPlayers.add(userId);
        pregameDB.set(userId, System.currentTimeMillis() + (mins * 60 * 1000) + 1000); //add a second so it looks nice
        pregameDB.set("innedPlayers", innedPlayers);
        postSignUps();
    }

    /**
     * Call this to remove a user from a signup list
     *
     * @param userId discord id of the user signing out
     */
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

    /**
     * Call this to post the current list of signed up players
     */
    @SuppressWarnings("unchecked")
    public void postSignUps() {
        if (gameGoing) return; //dont do this while there's a game going
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

    /**
     * Removes players that went beyond their sign up time
     */
    @SuppressWarnings("unchecked")
    private void clearSignUpList() {
        if (gameGoing) return; //dont do this while there's a game going
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

    @Override
    public void handleCommand(CommandParser.CommandContainer cmd) {
        if (commands.containsKey(cmd.invoke)) {
            Command c = commands.get(cmd.invoke);
            boolean safe = c.argumentsValid(cmd.args, cmd.event);

            if (safe) c.execute(cmd.args, cmd.event);
            c.executed(safe, cmd.event);
        }
    }

    /**
     * @return The channel where this Pregame is running in
     */
    public TextChannel getChannel() {
        return channel;
    }

    /**
     * @return The Listener object of this Pregame
     */
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
        if (!pregame.getChannel().getId().equals(event.getChannel().getId())) {
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
