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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by napster on 21.05.17.
 * <p>
 * contains various emojis used in the bot
 */
public class Emojis {

    private static final Logger log = LoggerFactory.getLogger(Emojis.class);

    //standard unicode based ones
    public static final String GUN = "🔫";
    public static final String WOLF = "🐺";
    public static final String POPCORN = "🍿";
    public static final String COFFIN = "⚰";
    public static final String SKULL = "💀";
    public static final String HOG = "🐷";
    public static final String POTATO = "🥔";
    public static final String VIDEO_GAME = "🎮";
    public static final String END = "🔚";
    public static final String COWBOY = "🤠";

    public static final String SUNNY = "☀";
    public static final String FULL_MOON = "🌕";
    public static final String CITY_SUNSET_SUNRISE = "🌇";


    //custom ones, currently hosted in the Wolfia Lounge
    public static final String EEK = "<:eek:318452576850804748>";
    public static final String CANTLOOK = "<:cantlook:318452650029088779>";
    public static final String RIP = "<:rip:318205519724675072>";
    public static final String WOLFTHINK = "<:wolfthinking:320350239795970048>";


    //to be called by eval for a quick'n'dirty test whether all emojis that are defined in this class are being
    //displayed in the Discord client as expected
    @SuppressWarnings("unused")
    public static String test() {
        final StringBuilder result = new StringBuilder();
        Arrays.stream(Emojis.class.getFields()).filter(field -> field.getType().equals(String.class)).forEach(field -> {
            try {
                result.append(field.get(null));
            } catch (final IllegalAccessException e) {
                result.append("exception");
                log.error("something something unexpected error while using reflection", e);
            }
        });
        return result.toString();
    }
}
