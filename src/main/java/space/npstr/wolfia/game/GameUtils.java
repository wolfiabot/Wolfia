/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.dv8tion.jda.api.entities.User;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.setup.StatusCommand;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Helpful mehtods for running games
 */
public class GameUtils {

    /**
     * @param items items, from which one is to be randed
     * @param <E>   class of the provided items and the desired returned one
     * @return a random item of the provided ones
     */
    public static <E> E rand(Collection<E> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Can't pick an item from zero items.");
        }
        int rand = ThreadLocalRandom.current().nextInt(items.size());
        int i = 0;
        E result = null;
        for (E item : items) {
            if (i == rand) {
                result = item;
                break;
            }
            i++;
        }
        return result;
    }

    /**
     * Objects vote for candidates
     * <p>
     * Find the candidate C with the most votes
     * If there are no votes, pick a random C from all candidates
     * If there is a tie, the one who was voted first will get selected
     * Make sure to provide votes in an ordered Map (LinkedHashMap for example) for this to work properly
     *
     * @return the candidates C with the most votes
     */
    public static <C> List<C> mostVoted(Map<?, C> votes, Collection<C> allCandidates) {
        long mostVotes = 0;
        Map<C, Long> votesAmountToCandidate = new HashMap<>(allCandidates.size());
        for (C candidate : new HashSet<>(allCandidates)) {
            long votesAmount = votes.values().stream().filter(candidate::equals).count();
            votesAmountToCandidate.put(candidate, votesAmount);
            if (votesAmount > mostVotes) {
                mostVotes = votesAmount;
            }
        }

        long most = mostVotes;
        List<C> result = new ArrayList<>();
        for (Map.Entry<C, Long> entry : votesAmountToCandidate.entrySet()) {
            C candidate = entry.getKey();
            long votesAmount = entry.getValue();
            if (votesAmount == most) result.add(candidate);
        }

        return result;
    }

    /**
     * @return highest amount of votes in any single candidate
     */
    public static <C> long mostVotes(Map<?, C> votes) {
        long mostVotes = 0;
        for (C candidate : new HashSet<>(votes.values())) {
            long votesAmount = votes.values().stream().filter(candidate::equals).count();
            if (votesAmount > mostVotes) {
                mostVotes = votesAmount;
            }
        }
        return mostVotes;
    }


    public static <O> Map<String, O> mapToStrings(Collection<O> objects, List<String> strings) {
        if (objects.size() >= strings.size()) {
            throw new IllegalArgumentException("Too many objects to map them to emojis.");
        }
        Map<String, O> mapped = new LinkedHashMap<>();//linked to preserve the order
        int i = 0;
        for (O object : objects) {
            mapped.put(strings.get(i), object);
            i++;
        }
        return mapped;
    }

    /**
     * @return the exact player found, or null and post a message
     */
    public static Player identifyPlayer(Collection<Player> players, CommandContext context) {
        List<Player> found = findPlayer(players, context);

        String explanation = String.format("Please use a mention or the player number which you can find with " +
                "`%s` so that I can clearly know who you are targeting.", WolfiaConfig.DEFAULT_PREFIX + StatusCommand.TRIGGER);
        if (found.isEmpty()) {
            context.replyWithMention("could not identify a player in your command! " + explanation);
            return null;
        }
        if (found.size() > 1) {
            context.replyWithMention("found more than one player for `"
                    + TextchatUtils.defuseMentions(context.rawArgs) + "`. " + explanation);
            return null;
        }

        return found.get(0);
    }

    /**
     * Tries to identify a player through various methods.
     * Will return an empty list if no match was found, a list with a single play if there was one match, or a list with
     * more than one of player in case of more than one hit. It is up to the caller to handle the cases.
     */
    public static List<Player> findPlayer(Collection<Player> players, CommandContext context, int... levenshteinThreshold) {

        //by mention
        for (User u : context.msg.getMentions().getUsers()) {
            Optional<Player> maybe = players.stream().filter(player -> player.userId == u.getIdLong()).findAny();
            if (maybe.isPresent()) {
                return Collections.singletonList(maybe.get());
            }
        }

        String input = context.rawArgs;
        //by number
        try {
            int number = Integer.parseInt(input);
            Optional<Player> maybe = players.stream().filter(player -> player.number == number).findAny();
            if (maybe.isPresent()) {
                return Collections.singletonList(maybe.get());
            }
        } catch (NumberFormatException ignored) {
            // ignored
        }

        //by userid
        try {
            long userId = Long.parseLong(input);
            Optional<Player> maybe = players.stream().filter(player -> player.userId == userId).findAny();
            if (maybe.isPresent()) {
                return Collections.singletonList(maybe.get());
            }
        } catch (NumberFormatException ignored) {
            // ignored
        }


        //levenshtein test of name and nicks of the players
        Map<Player, Integer> distances = new HashMap<>();
        for (Player p : players) {
            int distanceName = TextchatUtils.levenshteinDist(p.getName(), input);
            int distanceNick = TextchatUtils.levenshteinDist(p.getNick(), input);
            distances.put(p, Math.min(distanceName, distanceNick));
        }
        int smallestDistance = Integer.MAX_VALUE;
        for (Map.Entry<Player, Integer> entry : distances.entrySet()) {
            int distance = entry.getValue();
            if (distance < smallestDistance) {
                smallestDistance = distance;
            }
        }
        int threshold = levenshteinThreshold.length > 0 ? levenshteinThreshold[0] : 2;
        if (smallestDistance > threshold) {
            return Collections.emptyList(); //no player found
        } else {
            List<Player> result = new ArrayList<>();
            for (Map.Entry<Player, Integer> entry : distances.entrySet()) {
                Player p = entry.getKey();
                int distance = entry.getValue();
                if (distance == smallestDistance) result.add(p);
            }
            return result;
        }
    }

    private GameUtils() {}
}
