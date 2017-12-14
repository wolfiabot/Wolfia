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

package space.npstr.wolfia.game.definitions;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.utils.discord.Emojis;

import javax.annotation.Nonnull;

/**
 * Created by napster on 14.12.17.
 * <p>
 * Created for use with the Santa role
 * <p>
 * Type like class to represent a present + it's source. the source should be a userId
 */
public class Item {
    public final long sourceId;
    public final Items item;

    public Item(final long sourceId, @Nonnull final Items item) {
        this.sourceId = sourceId;
        this.item = item;
    }

    public enum Items {

        //may contain other items
        PRESENT(Emojis.PRESENT, String.format("You may open the present in your DMs with `%s`, or shorter `%s`",
                Config.PREFIX + CommRegistry.COMM_TRIGGER_OPENPRESENT, Config.PREFIX + CommRegistry.COMM_TRIGGER_OPENPRESENT_ALIAS)),


        //may shoot a player
        GUN(Emojis.GUN, String.format("You may shoot a player during the day in your DMs with `%s`"
                        + "\nSay `%s` to get a list of living players.",
                Config.PREFIX + CommRegistry.COMM_TRIGGER_SHOOT, Config.PREFIX + CommRegistry.COMM_TRIGGER_STATUS)),

        //may check a player
        MAGNIFIER(Emojis.MAGNIFIER, String.format("You may check a player's alignment during the night in your DMs with `%s`"
                        + "\nSay `%s` to get a list of living players.",
                Config.PREFIX + CommRegistry.COMM_TRIGGER_CHECK, Config.PREFIX + CommRegistry.COMM_TRIGGER_STATUS)),

        // kills the player holding it
        BOMB(Emojis.BOMB, "Kills you immediately " + Emojis.BOOM),

        //
        ;

        @Nonnull
        public final String emoji;

        @Nonnull
        public final String explanation;

        Items(@Nonnull final String emoji, @Nonnull final String explanation) {
            this.emoji = emoji;
            this.explanation = explanation;
        }


        @Override
        public String toString() {
            return this.emoji;
        }
    }
}
