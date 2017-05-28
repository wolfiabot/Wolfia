///*
// * Copyright (C) 2017 Dennis Neufeld
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published
// * by the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package space.npstr.wolfia.game;
//
//import net.dv8tion.jda.core.entities.TextChannel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import space.npstr.wolfia.Wolfia;
//import space.npstr.wolfia.db.DBWrapper;
//import space.npstr.wolfia.db.Player;
//
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
///**
// * Created by npstr on 24.08.2016
// */
//public class Pregame {
//
//    public static final Map<String, Pregame> REGISTRY = new HashMap<>();
//
//    private Set<String> innedPlayers;
//    private Set<String> confirmedPlayers;
//    private final TextChannel channel;
//    private final PregameListener listener;
//    private final static Logger log = LoggerFactory.getLogger(Pregame.class);
//
//    private final DBWrapper pregameDB;
//
//    public Pregame(final TextChannel channel, final DBWrapper db) {
//        this.channel = channel;
//        this.pregameDB = db;
//        this.listener = new PregameListener(this);
//        this.innedPlayers = getInnedPlayers();
//
//        Wolfia.handleOutputMessage(channel, "Bot started, signups now open in this channel");
//        postSignUps();
//
//        //this will keep track of players falling out of signups after a time
//        final Runnable checkSignupListEvery60Sec = () -> {
//            try {
//                while (true) {
//                    TimeUnit.SECONDS.sleep(60);
//                    if (this.gameGoing) continue; //dont need to do this when the thread is busy
//                    log.trace("check signup list task executing");
//                    clearSignUpList();
//                }
//
//            } catch (final InterruptedException e) {
//                e.printStackTrace(); //TODO handle interrupte properly
//            }
//        };
//        final Thread thread = new Thread(checkSignupListEvery60Sec);
//        thread.start();
//    }
//
//
////    /**
////     * Starts the confirmations phase, where there is a time frame during which the signed up players have to confirm
////     * their presence so the game can start with no afks. Other players may hero in, but it should be considered bad
////     * manners to do so before the original sign ups had enough time to confirm. Community needs to sort this out.
////     */
////    private void startConfirmations() {
////        this.confirmedPlayers = new HashSet<>();
////        String mentions = "";
////        for (final String userId : getInnedPlayers()) {
////            mentions += Player.asMention(userId) + " ";
////            Wolfia.handleOutputMessage(userId, "Hey " + Player.getDiscordNick(userId) + ", a game you signed up for is " +
////                    "about to start in " + this.channel.getAsMention() + ", please post " + this.PREFIX + ConfirmCommand.COMMAND +
////                    " in there during the next 10 minutes.");
////        }
////        Wolfia.handleOutputMessage(this.channel, "Enough players have signed up! Game start initiated. You have 10 minutes " +
////                "to confirm that you are in, just type _" + Config.PREFIX + ConfirmCommand.COMMAND + "_. Game starts as soon " +
////                "as enough players confirmed.\n" + mentions);
////
////        //this will hitler around every minute mentioning players we are waiting on
////        final Runnable hitlerConfirmations = () -> {
////            int times = 9;
////            try {
////                while (times > 0) {
////                    if (this.gameGoing) break; //if the game started we dont need this
////                    times--;
////                    TimeUnit.SECONDS.sleep(60);
////                    log.trace("hitlering confirmations");
////                    String out = "Oi! These players have confirmed: ";
////                    for (final String s : this.confirmedPlayers) out += Player.getDiscordNick(s) + " ";
////                    out += "\nNeed " + (this.game.getAmountsOfPlayers() - this.confirmedPlayers.size()) + " more players.\n";
////                    for (final String s : getInnedPlayers())
////                        if (!this.confirmedPlayers.contains(s)) out += Player.asMention(s) + " ";
////
////                    Wolfia.handleOutputMessage(this.channel, out);
////                }
////
////            } catch (final InterruptedException e) {
////                e.printStackTrace();//TODO handle interrupted properly
////            }
////        };
////        final Thread thread = new Thread(hitlerConfirmations);
////        thread.start();
////
////        //this will check if confirmations worked out
////        //possible bug if the game is over in less than 10 mins, then this thread will think the game never started
////        final Runnable checkConfirmations = () -> {
////            try {
////                TimeUnit.SECONDS.sleep(600);
////                log.trace("checking confirmations");
////                if (this.gameGoing) return;
////                else {
////                    String out = "Game start aborted. Shame on these players that signed up but did not confirm:\n";
////                    for (final String s : getInnedPlayers())
////                        if (!this.confirmedPlayers.contains(s)) out += Player.asMention(s) + " ";
////                    Wolfia.handleOutputMessage(this.channel, out);
////                }
////
////            } catch (final InterruptedException e) {
////                e.printStackTrace(); //TODO handle interrupted properly
////            }
////        };
////        final Thread thread2 = new Thread(checkConfirmations);
////        thread2.start();
////    }
//
//    //    /**
////     * Call this to have a user confirm his participation for an upcoming game
////     *
////     * @param userId discord id of the user that is confirming
////     */
////    public void confirm(final String userId) {
////        if (!this.confirmedPlayers.contains(userId)) {
////            this.confirmedPlayers.add(userId);
////            if (this.confirmedPlayers.size() == this.game.getAmountsOfPlayers()) {
////                //TODO open the channel after a game is over
////                String out = "Enough players confirmed. Game is starting!\n";
////                for (final String s : this.confirmedPlayers) {
////                    out += Player.asMention(s) + " ";
////                }
////                Wolfia.handleOutputMessage(this.channel, out);
////                this.gameGoing = true;
////                //remove confirm command
////                this.commands.remove(ConfirmCommand.COMMAND);
////                this.game.start(this.confirmedPlayers);
////            }
////        }
////    }
//    private Set<String> getInnedPlayers() {
//        this.innedPlayers = this.pregameDB.get("innedPlayers", Set.class);
//        if (this.innedPlayers == null) this.innedPlayers = new HashSet<>();
//        this.pregameDB.set("innedPlayers", this.innedPlayers);
//        return this.innedPlayers;
//    }
//
//    /**
//     * Call this to put a user on the signup list
//     *
//     * @param userId discord id of the user signing up
//     * @param mins   amount of minutes the user is signing up for
//     */
//    public void inPlayer(final String userId, final long mins) {
//        this.innedPlayers = getInnedPlayers();
//        this.innedPlayers.add(userId);
//        this.pregameDB.set(userId, System.currentTimeMillis() + (mins * 60 * 1000) + 1000); //add a second so it looks nice
//        this.pregameDB.set("innedPlayers", this.innedPlayers);
//        postSignUps();
//    }
//
//    /**
//     * Call this to remove a user from a signup list
//     *
//     * @param userId discord id of the user signing out
//     */
//    public void outPlayer(final String userId) {
//        this.innedPlayers = getInnedPlayers();
//        final boolean wasInned = this.innedPlayers.remove(userId);
//        this.pregameDB.del(userId);
//        this.pregameDB.set("innedPlayers", this.innedPlayers);
//        if (wasInned) {
//            Wolfia.handleOutputMessage(this.channel, Player.asMention(userId) + " outed.");
//            postSignUps();
//        } else {
//            Wolfia.handleOutputMessage(this.channel, Player.asMention(userId) + " you aren't even inned bro.");
//        }
//
//    }
//
//    /**
//     * Call this to post the current list of signed up players
//     */
//    public void postSignUps() {
//        if (this.gameGoing) return; //dont do this while there's a game going
//        this.innedPlayers = getInnedPlayers();
//        String output = "Current signups: " + this.innedPlayers.size() + " players\n";
//        for (final String userId : this.innedPlayers) {
//            final Long till = this.pregameDB.get(userId, Long.class);
//
//            long diff = till - System.currentTimeMillis();
//            diff /= 1000;
//            long mins = diff / 60;
//            final long hours = mins / 60;
//            mins = mins % 60;
//            output += "   " + Player.getDiscordNick(userId) + " for " + hours + "h " + mins + "m\n";
//        }
//
//        Wolfia.handleOutputMessage(this.channel, output);
//    }
//
//    /**
//     * Removes players that went beyond their sign up time
//     */
//    private void clearSignUpList() {
//        if (this.gameGoing) return; //dont do this while there's a game going
//        final List<String> gtfo = new ArrayList<>();
//        this.innedPlayers = getInnedPlayers();
//        for (final String userId : this.innedPlayers) {
//            final long till = this.pregameDB.get(userId, Long.class);
//            if (till < System.currentTimeMillis()) {
//                gtfo.add(userId);
//                continue;
//            }
//
//            final long lastSeen = Player.lastSeen(userId);
//            Long lastWarning = this.pregameDB.get("warningSent:" + userId, Long.class);
//            if (lastWarning == null) lastWarning = 0L;
//
//            if ((System.currentTimeMillis() - lastSeen > 3600000) & // 1hour of inactivity
//                    (System.currentTimeMillis() - lastWarning) > 600000) //10 mins since last warning
//            {
//                Wolfia.handleOutputMessage(userId, "Heya " + Player.getDiscordNick(userId) + ", you signed up for a" +
//                        "turbo, but I haven't seen you around for a while. You will be removed from the signup list " +
//                        "in 5 minutes due to inactivity. To prevent that write me a message here or on the Mafia " +
//                        "Universe Discord Server.");
//                this.pregameDB.set("warningSent:" + userId, System.currentTimeMillis());
//            }
//
//            if (System.currentTimeMillis() - lastSeen > 3900000) // 1hour 5 mins of inactivity
//            {
//                gtfo.add(userId);
//            }
//        }
//
//        for (final String userId : gtfo) {
//            this.innedPlayers.remove(userId);
//            this.pregameDB.set("innedPlayers", this.innedPlayers);
//            this.pregameDB.del(userId);
//            Wolfia.handleOutputMessage(this.channel, Player.asMention(userId) + " removed from signups");
//            Wolfia.handleOutputMessage(userId, "removed you from the signup list");
//        }
//        if (gtfo.size() > 0) {
//            postSignUps();
//        }
//    }
//
//    /**
//     * @return The channel where this Pregame is running in
//     */
//    public TextChannel getChannel() {
//        return this.channel;
//    }
//
//    /**
//     * @return The Listener object of this Pregame
//     */
//    public PregameListener getListener() {
//        return this.listener;
//    }
//}
