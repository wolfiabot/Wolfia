/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

import javax.annotation.Nonnull;
import space.npstr.wolfia.commands.ingame.CheckCommand;
import space.npstr.wolfia.commands.ingame.OpenPresentCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.setup.StatusCommand;
import space.npstr.wolfia.utils.discord.Emojis;

/**
 * Created by napster on 14.12.17.
 * <p>
 * Created for use with the Santa role
 * <p>
 * Type like class to represent a present + it's source. the source should be a userId
 */
public class Item {
    public final long sourceId;
    public final ItemType itemType;

    public Item(final long sourceId, @Nonnull final ItemType itemType) {
        this.sourceId = sourceId;
        this.itemType = itemType;
    }

    public enum ItemType {

        //may contain other items
        PRESENT(Emojis.PRESENT, String.format("You may open the present in your DMs with `%s`, or shorter `%s`",
                WolfiaConfig.DEFAULT_PREFIX + OpenPresentCommand.TRIGGER, WolfiaConfig.DEFAULT_PREFIX + OpenPresentCommand.ALIAS)),


        //may shoot a player
        GUN(Emojis.GUN, String.format("You may shoot a player during the day in your DMs with `%s`"
                        + "\nSay `%s` to get a list of living players.",
                WolfiaConfig.DEFAULT_PREFIX + ShootCommand.TRIGGER, WolfiaConfig.DEFAULT_PREFIX + StatusCommand.TRIGGER)),

        //may check a player
        MAGNIFIER(Emojis.MAGNIFIER, String.format("You may check a player's alignment during the night in your DMs with `%s`"
                        + "\nSay `%s` to get a list of living players.",
                WolfiaConfig.DEFAULT_PREFIX + CheckCommand.TRIGGER, WolfiaConfig.DEFAULT_PREFIX + StatusCommand.TRIGGER)),

        // kills the player holding it
        BOMB(Emojis.BOMB, "Kills you immediately " + Emojis.BOOM),

        // protect the player from death (but not from lynch)
        ANGEL(Emojis.ANGEL, "You will be protected from death once, but not from getting lynched."),

        //
        ;

        @Nonnull
        public final String emoji;

        @Nonnull
        public final String explanation;

        ItemType(@Nonnull final String emoji, @Nonnull final String explanation) {
            this.emoji = emoji;
            this.explanation = explanation;
        }


        @Override
        public String toString() {
            return this.emoji;
        }
    }
}
