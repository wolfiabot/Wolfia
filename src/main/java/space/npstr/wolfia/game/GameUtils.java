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

import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
     * @return the candidate C with the most votes
     */
    public static <C> C mostVoted(final Map<?, C> votes, final Collection<C> allCandidates) {
        long mostVotes = 0;
        C winningCandidate = GameUtils.rand(allCandidates); //default candidate is a rand
        for (final C candidate : votes.values()) {
            final long votesAmount = votes.values().stream().filter(candidate::equals).count();
            if (votesAmount > mostVotes) {
                mostVotes = votesAmount;
                winningCandidate = candidate;
            }
        }
        return winningCandidate;
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
}
