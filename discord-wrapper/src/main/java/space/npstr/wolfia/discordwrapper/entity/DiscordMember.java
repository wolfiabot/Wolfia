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

import net.dv8tion.jda.api.entities.Member;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Data container for a discord member
 */
@Value.Immutable
public interface DiscordMember {

    static DiscordMember from(Member member) {
        return ImmutableDiscordMember.builder()
                .user(DiscordUser.from(member.getUser()))
                .guildId(member.getGuild().getIdLong())
                .nickname(Optional.ofNullable(member.getNickname()))
                .build();
    }

    default String effectiveName() {
        return nickname().orElse(user().name());
    }

    DiscordUser user();

    long guildId();

    Optional<String> nickname();
}
