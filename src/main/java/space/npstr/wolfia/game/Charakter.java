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

import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Roles;

/**
 * deliberate misspelling with the german word for character to avoid confusion with Java's Character class
 */
public class Charakter {
    public final Alignments alignment;
    public final Roles role;

    public Charakter(Alignments alignment, Roles role) {
        this.alignment = alignment;
        this.role = role;
    }
}
