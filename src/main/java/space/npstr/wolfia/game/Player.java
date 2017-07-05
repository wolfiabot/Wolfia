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

import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.utils.IllegalGameStateException;

/**
 * Created by napster on 05.07.17.
 * <p>
 * Representing a player in a game
 */
public class Player {

    public final long userId;
    public final boolean isWolf;
    public final Roles role;

    private boolean isLiving = true;

    public Player(final long userId, final boolean isWolf, final Roles role) {
        this.userId = userId;
        this.isWolf = isWolf;
        this.role = role;
    }

    public boolean isLiving() {
        return this.isLiving;
    }

    public void kill() throws IllegalGameStateException {
        if (!this.isLiving) {
            throw new IllegalGameStateException("Can't kill a dead player");
        }
        this.isLiving = false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.userId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Player)) return false;
        final Player other = (Player) obj;

        return other.userId == this.userId;
    }
}
