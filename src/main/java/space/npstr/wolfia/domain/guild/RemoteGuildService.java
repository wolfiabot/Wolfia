/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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

package space.npstr.wolfia.domain.guild;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.domain.discord.DiscordRequester;
import space.npstr.wolfia.domain.discord.PartialGuild;
import space.npstr.wolfia.webapi.WebUser;

import static java.util.Optional.ofNullable;

/**
 * Fetch guilds on behalf of a user from Discord
 */
@Component
public class RemoteGuildService {

    private static final Permission EDIT_PERMISSION = Permission.ADMINISTRATOR;
    private static final Duration CACHE_DURATION = Duration.ofSeconds(30);

    private final DiscordRequester discordRequester;
    private final ShardManager shardManager;

    private final Cache<WebUser, List<PartialGuild>> cache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_DURATION)
            .build();

    public RemoteGuildService(DiscordRequester discordRequester, ShardManager shardManager) {
        this.discordRequester = discordRequester;
        this.shardManager = shardManager;
    }

    public Action asUser(WebUser webUser) {
        return new Action(webUser);
    }

    public class Action {
        private final WebUser webUser;

        private Action(WebUser webUser) {
            this.webUser = webUser;
        }

        public Optional<GuildInfo> fetchGuild(long guildId) {
            return fetchAllGuilds().stream()
                    .filter(guildInfo -> guildInfo.guild().id() == guildId)
                    .findAny();
        }

        public List<GuildInfo> fetchAllGuilds() {
            return ofNullable(cache.get(this.webUser, user -> discordRequester.fetchAllGuilds(user.accessToken().getTokenValue())))
                    .orElseGet(List::of)
                    .stream()
                    .map(partialGuild -> toGuildInfo(partialGuild, this.webUser.id()))
                    .collect(Collectors.toList());
        }

        private GuildInfo toGuildInfo(PartialGuild partialGuild, long userId) {
            Guild guild = shardManager.getGuildCache().getElementById(partialGuild.id());

            boolean canEdit = false;
            if (guild != null) {
                Member member = guild.getMemberById(userId);
                if (member != null) {
                    canEdit = member.hasPermission(EDIT_PERMISSION);
                }
            }
            return ImmutableGuildInfo.builder()
                    .guild(partialGuild)
                    .botPresent(guild != null)
                    .canEdit(canEdit)
                    .build();
        }
    }
}
