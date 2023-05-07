/*
 * Copyright (C) 2016-2020 the original author or authors
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

package space.npstr.wolfia.utils.discord;

import java.util.Arrays;
import java.util.List;

/**
 * Contains various emojis used in the bot
 */
public class Emojis {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Emojis.class);

    //standard unicode based ones
    public static final String GUN = "🔫";
    public static final String WOLF = "🐺";
    public static final String COFFIN = "⚰";
    public static final String SKULL = "💀";
    public static final String VIDEO_GAME = "🎮";
    public static final String END = "🔚";
    public static final String COWBOY = "🤠";
    public static final String ROCKET = "🚀";
    public static final String BOOM = "💥";
    public static final String OK_HAND = "👌";
    public static final String WINK = "😉";
    public static final String ONE_HUNDRED = "💯";
    public static final String SLEEP = "💤";
    public static final String STOP = "🛑";
    public static final String TOOLS = "🛠️";
    public static final String CHECKERED_FLAG = "🏁";

    public static final String SUNNY = "☀";
    public static final String FULL_MOON = "🌕";
    public static final String CITY_SUNSET_SUNRISE = "🌇";

    public static final String X = "❌";
    public static final String CHECK = "✅";
    public static final String WARN = "⚠️";

    public static final String BALLOT_BOX = "🗳";
    public static final String FIRE = "🔥";
    public static final String MAGNIFIER = "🔍";
    public static final String SPY = "🕵";
    public static final String ANGRY_BUBBLE = "🗯";
    public static final String SANTA = "🎅";
    public static final String BOMB = "💣";
    public static final String PRESENT = "🎁";
    public static final String ANGEL = "👼";


    public static final String NUMBER_0 = "0⃣";
    public static final String NUMBER_1 = "1⃣";
    public static final String NUMBER_2 = "2⃣";
    public static final String NUMBER_3 = "3⃣";
    public static final String NUMBER_4 = "4⃣";
    public static final String NUMBER_5 = "5⃣";
    public static final String NUMBER_6 = "6⃣";
    public static final String NUMBER_7 = "7⃣";
    public static final String NUMBER_8 = "8⃣";
    public static final String NUMBER_9 = "9⃣";
    public static final String NUMBER_10 = "🔟";

    public static final List<String> NUMBERS = List.of(NUMBER_0, NUMBER_1, NUMBER_2, NUMBER_3, NUMBER_4,
            NUMBER_5, NUMBER_6, NUMBER_7, NUMBER_8, NUMBER_9, NUMBER_10);

    public static final String LETTER_A = "🇦";
    public static final String LETTER_B = "🇧";
    public static final String LETTER_C = "🇨";
    public static final String LETTER_D = "🇩";
    public static final String LETTER_E = "🇪";
    public static final String LETTER_F = "🇫";
    public static final String LETTER_G = "🇬";
    public static final String LETTER_H = "🇭";
    public static final String LETTER_I = "🇮";
    public static final String LETTER_J = "🇯";
    public static final String LETTER_K = "🇰";
    public static final String LETTER_L = "🇱";
    public static final String LETTER_M = "🇲";
    public static final String LETTER_N = "🇳";
    public static final String LETTER_O = "🇴";
    public static final String LETTER_P = "🇵";
    public static final String LETTER_Q = "🇶";
    public static final String LETTER_R = "🇷";
    public static final String LETTER_S = "🇸";
    public static final String LETTER_T = "🇹";
    public static final String LETTER_U = "🇺";
    public static final String LETTER_V = "🇻";
    public static final String LETTER_W = "🇼";
    public static final String LETTER_X = "🇽";
    public static final String LETTER_Y = "🇾";
    public static final String LETTER_Z = "🇿";

    public static final List<String> LETTERS = List.of(LETTER_A, LETTER_B, LETTER_C, LETTER_D, LETTER_E, LETTER_F,
            LETTER_G, LETTER_H, LETTER_I, LETTER_J, LETTER_K, LETTER_L, LETTER_M, LETTER_N, LETTER_O, LETTER_P,
            LETTER_Q, LETTER_R, LETTER_S, LETTER_T, LETTER_U, LETTER_V, LETTER_W, LETTER_X, LETTER_Y, LETTER_Z);

    //custom ones, currently hosted in the Wolfia Lounge
    public static final String EEK = "<:eek:318452576850804748>";
    public static final String CANTLOOK = "<:cantlook:318452650029088779>";
    public static final String RIP = "<:rip:318205519724675072>";
    public static final String WOLFTHINK = "<:wolfthinking:323519533765754890>";


    //to be called by eval for a quick'n'dirty test whether all emojis that are defined in this class are being
    //displayed in the Discord client as expected
    @SuppressWarnings("unused")
    public static String test() {
         StringBuilder result = new StringBuilder();
        Arrays.stream(Emojis.class.getFields()).filter(field -> field.getType().equals(String.class)).forEach(field -> {
            try {
                result.append(field.get(null));
            } catch (IllegalAccessException e) {
                result.append("exception");
                log.error("something something unexpected error while using reflection", e);
            }
            result.append(" ");
        });
        return result.toString();
    }

    private Emojis() {}
}
