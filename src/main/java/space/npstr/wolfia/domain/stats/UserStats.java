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

package space.npstr.wolfia.domain.stats;

import static org.immutables.value.Value.Immutable;

@Immutable
@StatsStyle
public interface UserStats {

    /**
     * @return id of the user these stats belong to
     */
    long userId();

    /**
     * @return total amount of games this user participated in
     */
    long totalGames();

    /**
     * @return amount of games this user won
     */
    long gamesWon();

    /**
     * @return amount of games this user played as a baddie
     */
    long gamesAsBaddie();

    /**
     * @return amount of games this user played as a baddie and won
     */
    long gamesWonAsBaddie();

    /**
     * @return amount of games this user played as a goodie
     */
    long gamesAsGoodie();

    /**
     * @return amount of games this user played as a goodie and won
     */
    long gamesWonAsGoodie();

    /**
     * @return total shots by this player
     */
    long totalShots();

    /**
     * @return amount of wolves shot by this user
     */
    long wolvesShot();

    /**
     * @return total posts written by this user
     */
    long totalPosts();

    /**
     * @return total length of posts written by this user
     */
    long totalPostLength();
}
