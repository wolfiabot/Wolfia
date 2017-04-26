/*
 * Copyright (C) 2017 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.utils;

import space.npstr.wolfia.Main;

/**
 * Created by npstr on 23.08.2016
 * <p>
 * just provides static methods to access player data on the DB
 */
public class Player {

    private static DBWrapper db;

    private Player() {
    }

    public static void setDB(DBWrapper db) {
        Player.db = db;
    }

    public static String asMention(String userId) {
        return "<@" + userId + ">";
    }

    public static String getDiscordNick(String userId) {
        return Main.jda.getUserById(userId).getName();
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


    public static long lastSeen(String userId) {
        Long result = db.get("lastSeen:" + userId, Long.class);
        if (result == null) result = 0L;
        return result;
    }

    public static void justSeen(String userId) {
        db.set("lastSeen:" + userId, System.currentTimeMillis());
    }

}
