package de.npstr.wolfia.utils;

import de.npstr.wolfia.Main;

/**
 * Created by npstr on 23.08.2016
 * <p>
 * just provides static methods to access player data on the DB
 */
public class Player {

    private static DBWrapper db;

    private Player() {
    }

    static void setDB(DBWrapper db) {
        Player.db = db;
    }

    public static String asMention(String userId) {
        return "<@" + userId + ">";
    }

    public static String getDiscordNick(String userId) {
        return Main.jda.getUserById(userId).getUsername(); //TODO is calling this expensive?
    }

    public static int getSingups(String userId) {
        Integer singups = db.get("singups:" + userId, Integer.class);
        if (singups == null) singups = 0;
        return singups;
    }

    public static int singup(String userId) {
        int singups = getSingups(userId);
        singups++;
        db.set("singups:" + userId, singups);
        return singups;
    }
}
