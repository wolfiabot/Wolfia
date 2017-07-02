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

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
    public static <E> E rand(final Set<E> items) {
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
}
