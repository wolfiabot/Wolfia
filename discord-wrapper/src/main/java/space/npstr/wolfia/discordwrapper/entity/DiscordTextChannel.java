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

package space.npstr.wolfia.discordwrapper.entity;

import net.dv8tion.jda.api.entities.TextChannel;
import org.immutables.value.Value;

/**
 * Data container for a discord text channel
 */
@Value.Immutable
public interface DiscordTextChannel {

    static DiscordTextChannel from(TextChannel textChannel) {
        return ImmutableDiscordTextChannel.builder()
                .id(textChannel.getIdLong())
                .build();
    }

    long id();
}
