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

package space.npstr.wolfia.discordwrapper;

import space.npstr.wolfia.discordwrapper.entity.DiscordGuild;
import space.npstr.wolfia.discordwrapper.entity.DiscordMember;
import space.npstr.wolfia.discordwrapper.entity.DiscordPrivateChannel;
import space.npstr.wolfia.discordwrapper.entity.DiscordSelfUser;
import space.npstr.wolfia.discordwrapper.entity.DiscordTextChannel;
import space.npstr.wolfia.discordwrapper.entity.DiscordUser;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * Created by napster on 31.03.19.
 */
public interface DiscordEntityProvider {

    @CheckReturnValue
    Optional<DiscordUser> getUserById(long userId);

    //try getUserById first for a possibly faster resolution
    //NOTE: these might be fake users, and fake user should not be DMed
    @CheckReturnValue
    CompletionStage<Optional<DiscordUser>> retrieveUserById(long userId);

    //NOTE: these might be fake users, and fake user should not be DMed
    @CheckReturnValue
    CompletionStage<Optional<DiscordUser>> getOrRetrieveUserById(long userId);

    @CheckReturnValue
    Optional<DiscordMember> getMember(long userId, long guildId);

    @CheckReturnValue
    Optional<DiscordGuild> getGuildById(long guildId);

    @CheckReturnValue
    Optional<DiscordTextChannel> getTextChannelById(long channelId);

    @CheckReturnValue
    Optional<DiscordPrivateChannel> getPrivateChannelById(long channelId);

    @CheckReturnValue
    boolean allShardsUp();

    @CheckReturnValue
    Optional<DiscordSelfUser> self();

    @CheckReturnValue
    long countShards();

    @CheckReturnValue
    Stream<DiscordGuild> streamGuilds();

    @CheckReturnValue
    long countGuilds();

    void shutdown();

    // check guild -> bot users -> discord api
    @CheckReturnValue
    CompletionStage<String> retrieveNick(long userId, Optional<Long> guildId);

}
