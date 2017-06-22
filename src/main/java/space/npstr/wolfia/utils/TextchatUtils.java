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

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 21.05.17.
 * <p>
 * Useful methods for the Discord chat and general working with Strings and outputs
 */
public class TextchatUtils {

    public static String userAsMention(final long userId) {
        return "<@" + userId + ">";
    }

    public static String formatMillis(final long millis) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    public static String createInviteLink(final TextChannel channel, final Operation... onFail) {
        try {
            return "https://discord.gg/" + channel.createInvite().complete().getCode();
        } catch (final PermissionException ignored) {
            if (onFail.length > 0) {
                onFail[0].execute();
            }
            return "";
        }
    }

    private static final List<String> TRUE_TEXT = Arrays.asList("true", "yes", "enable", "y", "on", "1", "positive");
    private static final List<String> FALSE_TEXT = Arrays.asList("false", "no", "disable", "n", "off", "0", "negative");

    public static boolean isTrue(final String input) {
        return TRUE_TEXT.contains(input);
    }

    public static boolean isFalse(final String input) {
        return FALSE_TEXT.contains(input);
    }

    public static String percentFormat(final double value) {
        final NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }

    /**
     * @return performs a division; returns 0 if the divisor is 0
     */
    public static double divide(final long dividend, final long divisor) {
        if (divisor == 0) return 0;
        return 1.0 * dividend / divisor;
    }

    /**
     * Case insensitive
     * Useful for fuzzy matching of two strings
     * Source: https://rosettacode.org/wiki/Levenshtein_distance#Java
     * <p>
     * expected complexity: O(b + a*b)  (a and b = lengths of a and b)
     */
    public static int levenshteinDist(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        final int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                final int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    //just kept around to eval-test the above levenshtein code
    public static String levenshteinTest() {
        final String[] data = {"kitten", "sitting", "saturday", "sunday", "rosettacode", "raisethysword"};
        String out = "";
        for (int i = 0; i < data.length; i += 2)
            out += ("\nlevenshteinDist(" + data[i] + ", " + data[i + 1] + ") = " + levenshteinDist(data[i], data[i + 1]));
        return out;
    }
}
