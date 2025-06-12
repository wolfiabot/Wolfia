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

package space.npstr.wolfia.game.definitions;

import space.npstr.wolfia.utils.discord.Emojis;

/**
 * Possible alignments in the game
 */
public enum Alignments {
    VILLAGE("Village", String.format("Your alignment is **Villager** %s. Your goal is to kill all wolves.", Emojis.COWBOY),
            "Town", String.format("Your alignment is **Town** %s. Your goal is to kill all mafia.", Emojis.COWBOY)),
    WOLF("Wolves", String.format("Your alignment is **Wolf** %s. Your goal is to reach parity with the village.", Emojis.WOLF),
            "Mafia", String.format("Your alignment is **Mafia** %s. Your goal is to reach parity with town.", Emojis.SPY));


    //werewolf lingo
    public final String textRepWW;

    //alignment related building block of character pms
    public final String rolePmBlockWW;

    //mafia lingo
    public final String textRepMaf;

    //alignment related building block of character pms
    public final String rolePmBlockMaf;

    Alignments(String textRepWW, String rolePmBlockWW, String textRepMaf, String rolePmBlockMaf) {
        this.textRepWW = textRepWW;
        this.rolePmBlockWW = rolePmBlockWW;

        this.textRepMaf = textRepMaf;
        this.rolePmBlockMaf = rolePmBlockMaf;
    }
}
