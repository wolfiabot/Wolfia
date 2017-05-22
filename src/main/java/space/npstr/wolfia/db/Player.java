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

package space.npstr.wolfia.db;

/**
 * Created by npstr on 23.08.2016
 * <p>
 * Represents a Player
 */
public class Player {

    private static DBWrapper db;

    //this is a static helper class
    private Player() {
    }

    public static void setDB(final DBWrapper db) {
        Player.db = db;
    }


//    public static long lastSeen(String userId) {
//        Long result = db.get("lastSeen:" + userId, Long.class);
//        if (result == null) result = 0L;
//        return result;
//    }
//
//    public static void justSeen(String userId) {
//        db.set("lastSeen:" + userId, System.currentTimeMillis());
//    }

}
