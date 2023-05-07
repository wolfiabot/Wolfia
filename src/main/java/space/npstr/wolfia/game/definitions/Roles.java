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

package space.npstr.wolfia.game.definitions;

import space.npstr.wolfia.utils.discord.Emojis;

/**
 * Roles related stuff. Don't just change once created enums in production or things break
 */
public enum Roles {
    VANILLA("Vanilla", "Your role is **Vanilla**. Your vote is your weapon. "),
    COP("Cop", String.format("Your role is **Cop** %s. Each night you may check a player's alignment.", Emojis.MAGNIFIER)),
    SANTA("Santa", String.format("Your role is **Santa** %s. You may hand out a %s to a player each night.\n" +
                    "The present has a random chance to contain a %s, %s, %s or %s.", Emojis.SANTA,
            Item.ItemType.PRESENT, Item.ItemType.GUN, Item.ItemType.MAGNIFIER, Item.ItemType.BOMB, Item.ItemType.ANGEL));

    public final String textRep;

    //role related building block of character pms
    public final String rolePmBlock;

    Roles(String textRepresentation, String rolePmBlock) {
        this.textRep = textRepresentation;
        this.rolePmBlock = rolePmBlock;
    }
}
