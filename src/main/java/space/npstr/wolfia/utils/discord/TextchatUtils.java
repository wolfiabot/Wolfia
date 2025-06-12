/*
 * Copyright (C) 2016-2025 the original author or authors
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

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.lang.Nullable;
import space.npstr.wolfia.utils.Operation;

/**
 * Useful methods for the Discord chat and general working with Strings and outputs
 */
public class TextchatUtils {

    public static final String ZERO_WIDTH_SPACE = "\u200B";

    public static final DateTimeFormatter TIME_IN_BERLIN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z")
            .withZone(ZoneId.of("Europe/Berlin"));

    public static final int MAX_MESSAGE_LENGTH = 2000;
    public static final List<String> TRUE_TEXT = List.of("true", "yes", "enable", "y", "on", "1", "positive", "+",
            "add", "start", "join", "ja");
    public static final List<String> FALSE_TEXT = List.of("false", "no", "disable", "n", "off", "0", "negative", "-",
            "remove", "stop", "leave", "nein");

    public static boolean isTrue(String input) {
        return TRUE_TEXT.contains(input);
    }

    public static boolean isFalse(String input) {
        return FALSE_TEXT.contains(input);
    }

    public static String userAsMention(long userId) {
        return "<@" + userId + ">";
    }

    public static String formatMillis(long millis) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    //this will not always create a new invite, discord/JDA reuses previously created one
    //so no worries about spammed invites in a channel
    public static String getOrCreateInviteLinkForChannel(TextChannel channel, Operation... onFail) {
        try {
            return channel.createInvite().complete().getUrl();
        } catch (PermissionException ignored) {
            // ignored
        }
        try {
            List<Invite> invites = channel.retrieveInvites().complete();
            if (!invites.isEmpty()) return invites.get(0).getUrl();
        } catch (PermissionException ignored) {
            // ignored
        }

        // if we reached this point, we failed at creating an invite for this channel
        if (onFail.length > 0) {
            onFail[0].execute();
        }
        return "";
    }

    //a more aggressive variant of getOrCreateInviteLinkForChannel() which will try to create an invite anywhere into
    // a guild
    public static String getOrCreateInviteLinkForGuild(Guild guild, @Nullable TextChannel preferred,
                                                       Operation... onFail) {
        if (preferred != null) {
            String preferredInvite = getOrCreateInviteLinkForChannel(preferred);
            if (!preferredInvite.isEmpty()) return preferredInvite;
        }

        for (TextChannel tc : guild.getTextChannels()) {
            String invite = getOrCreateInviteLinkForChannel(tc);
            if (!invite.isEmpty()) return invite;
        }

        // if we reached this point, we failed at creating an invite for this guild
        if (onFail.length > 0) {
            onFail[0].execute();
        }
        return "";
    }

    public static String percentFormat(double value) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }

    /**
     * @return performs a division; returns 0 if the divisor is 0
     */
    public static double divide(long dividend, long divisor) {
        if (divisor == 0) return 0;
        return 1.0 * dividend / divisor;
    }

    /**
     * @return performs a division; returns 0 if the divisor is 0
     */
    public static double divide(int dividend, int divisor) {
        return divide((long) dividend, divisor);
    }


    /**
     * Case insensitive
     * Useful for fuzzy matching of two strings
     * Source: https://rosettacode.org/wiki/Levenshtein_distance#Java
     * <p>
     * expected complexity: O(b + a*b)  (a and b = lengths of a and b)
     */
    public static int levenshteinDist(String x, String y) {
        String a = x.toLowerCase();
        String b = y.toLowerCase();
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    /**
     * Runs the levenshtein distance algorithm over the provided strings
     *
     * @return returns true if the distance is equal or smaller than maxDist, false otherwise
     */
    public static boolean isSimilar(String x, String y, int maxDistance) {
        return levenshteinDist(x, y) <= maxDistance;
    }

    /**
     * Same as {@link TextchatUtils#isSimilar(String, String, int)}, just with a default maxDistance of 3
     */
    public static boolean isSimilar(String x, String y) {
        return isSimilar(x, y, 3);
    }

    /**
     * Same as {@link TextchatUtils#isSimilar(String, String)}, but forces string to be lower case before comparing them
     */
    public static boolean isSimilarLower(String x, String y) {
        return isSimilar(x.toLowerCase(), y.toLowerCase());
    }

    //just kept around to eval-test the above levenshtein code
    public static String levenshteinTest() {
        String[] data = {"kitten", "sitting", "saturday", "sunday", "rosettacode", "raisethysword"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < data.length; i += 2)
            out.append("\nlevenshteinDist(").append(data[i]).append(", ").append(data[i + 1]).append(") = ")
                    .append(levenshteinDist(data[i], data[i + 1]));
        return out.toString();
    }

    public static String asMarkdown(String str) {
        return "```md\n" + str + "```";
    }

    public static String toUtcTime(long epochMillis) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z").withZone(ZoneId.of("UTC"));
        return dtf.format(Instant.ofEpochMilli(epochMillis));
    }

    public static String toBerlinTime(long epochMillis) {
        return TIME_IN_BERLIN.format(Instant.ofEpochMilli(epochMillis));
    }

    public static String defuseMentions(String input) {
        return input.replace("@", "@" + ZERO_WIDTH_SPACE);
    }


    private static final List<Character> MARKDOWN_CHARS = Arrays.asList('*', '`', '~', '_');

    //thanks fredboat
    public static String escapeMarkdown(String str) {
        StringBuilder revisedString = new StringBuilder(str.length());
        for (Character n : str.toCharArray()) {
            if (MARKDOWN_CHARS.contains(n)) {
                revisedString.append("\\");
            }
            revisedString.append(n);
        }
        return revisedString.toString();
    }

    public static MessageCreateData prefaceWithName(User user, String msg, boolean escape) {
        String name = user.getName();
        if (escape) {
            name = escapeMarkdown(name);
        }
        return prefaceWithString(name, msg);
    }

    public static MessageCreateData prefaceWithName(Member member, String msg, boolean escape) {
        String name = member.getEffectiveName();
        if (escape) {
            name = escapeMarkdown(name);
        }
        return prefaceWithString(name, msg);
    }

    public static MessageCreateData prefaceWithMention(User user, String msg) {
        return prefaceWithString(user.getAsMention(), msg);
    }

    //thanks fredboat
    private static MessageCreateData prefaceWithString(String preface, String msg) {
        String message = ensureSpace(msg);
        return new MessageCreateBuilder()
                .addContent(preface)
                .addContent(",")
                .addContent(message)
                .build();
    }

    //thanks fredboat
    private static String ensureSpace(String msg) {
        return msg.charAt(0) == ' ' ? msg : " " + msg;
    }

    private TextchatUtils() {}
}
