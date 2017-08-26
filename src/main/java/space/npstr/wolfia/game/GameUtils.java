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

package space.npstr.wolfia.game;

import net.dv8tion.jda.core.entities.User;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by napster on 21.05.17.
 * <p>
 * Helpful mehtods for running games
 */
public class GameUtils {

    /**
     * @param items items, from which one is to be randed
     * @param <E>   class of the provided items and the desired returned one
     * @return a random item of the provided ones
     */
    public static <E> E rand(final Collection<E> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Can't pick an item from zero items.");
        }
        final int rand = ThreadLocalRandom.current().nextInt(items.size());
        int i = 0;
        E result = null;
        for (final E item : items) {
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
    public static <C> List<C> mostVoted(final Map<?, C> votes, final Collection<C> allCandidates) {
        long mostVotes = 0;
        final Map<C, Long> votesAmountToCandidate = new HashMap<>(allCandidates.size());
        for (final C candidate : allCandidates.stream().distinct().collect(Collectors.toSet())) {
            final long votesAmount = votes.values().stream().filter(candidate::equals).count();
            votesAmountToCandidate.put(candidate, votesAmount);
            if (votesAmount > mostVotes) {
                mostVotes = votesAmount;
            }
        }

        final long most = mostVotes;
        final List<C> result = new ArrayList<>();
        votesAmountToCandidate.forEach((candidate, votesAmount) -> {
            if (votesAmount == most) result.add(candidate);
        });

        return result;
    }

    /**
     * @return highest amount of votes in any single candidate
     */
    public static <C> long mostVotes(final Map<?, C> votes) {
        long mostVotes = 0;
        for (final C candidate : votes.values().stream().distinct().collect(Collectors.toSet())) {
            final long votesAmount = votes.values().stream().filter(candidate::equals).count();
            if (votesAmount > mostVotes) {
                mostVotes = votesAmount;
            }
        }
        return mostVotes;
    }


    public static <O> Map<String, O> mapToStrings(final Collection<O> objects, final List<String> strings) {
        if (objects.size() >= strings.size()) {
            throw new IllegalArgumentException("Too many objects to map them to emojis.");
        }
        final Map<String, O> mapped = new LinkedHashMap<>();//linked to preserve the order
        int i = 0;
        for (final O object : objects) {
            mapped.put(strings.get(i), object);
            i++;
        }
        return mapped;
    }


    public static List<String> asMentions(final Collection<Player> input) {
        return input.stream()
                .map(p -> TextchatUtils.userAsMention(p.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * @return the exact player found, or null and post a message
     */
    public static Player identifyPlayer(final Collection<Player> players, final CommandParser.CommandContainer commandInfo) {
        final List<Player> found = findPlayer(players, commandInfo);

        final String explanation = String.format("Please use a mention or the player number which you can find with " +
                "`%s` so that I can clearly know who you are targeting.", Config.PREFIX + VoteCountCommand.COMMAND);
        if (found.isEmpty()) {
            commandInfo.reply("%s, could not identify a player in your command! " + explanation,
                    commandInfo.invoker.getAsMention());
            return null;
        }
        if (found.size() > 1) {
            commandInfo.reply("%s, found more than one player for `%s`." + explanation,
                    commandInfo.invoker.getAsMention(), TextchatUtils.defuseMentions(commandInfo.argsRaw));
            return null;
        }

        return found.get(0);
    }

    /**
     * Tries to identify a player through various methods.
     * Will return an empty list if no match was found, a list with a single play if there was one match, or a list with
     * more than one of player in case of more than one hit. It is up to the caller to handle the cases.
     */
    public static List<Player> findPlayer(final Collection<Player> players, final CommandParser.CommandContainer commandInfo, final int... levenshteinThreshold) {

        //by mention
        for (final User u : commandInfo.event.getMessage().getMentionedUsers()) {
            final Optional<Player> maybe = players.stream().filter(player -> player.userId == u.getIdLong()).findAny();
            if (maybe.isPresent()) {
                return Collections.singletonList(maybe.get());
            }
        }

        final String input = commandInfo.argsRaw;
        //by number
        try {
            final int number = Integer.valueOf(input);
            final Optional<Player> maybe = players.stream().filter(player -> player.number == number).findAny();
            if (maybe.isPresent()) {
                return Collections.singletonList(maybe.get());
            }
        } catch (final NumberFormatException ignored) {
        }

        //by userid
        try {
            final long userId = Long.valueOf(input);
            final Optional<Player> maybe = players.stream().filter(player -> player.userId == userId).findAny();
            if (maybe.isPresent()) {
                return Collections.singletonList(maybe.get());
            }
        } catch (final NumberFormatException ignored) {
        }


        //levenshtein test of name and nicks of the players
        final Map<Player, Integer> distances = new HashMap<>();
        for (final Player p : players) {
            final int distanceName = TextchatUtils.levenshteinDist(p.getName(), input);
            final int distanceNick = TextchatUtils.levenshteinDist(p.getNick(), input);
            distances.put(p, distanceName < distanceNick ? distanceName : distanceNick);
        }
        int smallestDistance = Integer.MAX_VALUE;
        for (final Player p : distances.keySet()) {
            final int distance = distances.get(p);
            if (distance < smallestDistance) {
                smallestDistance = distance;
            }
        }
        final int threshold = levenshteinThreshold.length > 0 ? levenshteinThreshold[0] : 2;
        if (smallestDistance > threshold) {
            return Collections.emptyList(); //no player found
        } else {
            final List<Player> result = new ArrayList<>();
            for (final Player p : distances.keySet()) {
                if (distances.get(p) == smallestDistance) result.add(p);
            }
            return result;
        }
    }
}
