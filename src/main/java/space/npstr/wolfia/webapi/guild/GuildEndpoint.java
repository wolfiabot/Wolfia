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

package space.npstr.wolfia.webapi.guild;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import space.npstr.wolfia.db.type.OAuth2Scope;
import space.npstr.wolfia.domain.guild.RemoteGuildService;
import space.npstr.wolfia.webapi.BaseEndpoint;
import space.npstr.wolfia.webapi.WebUser;

public abstract class GuildEndpoint extends BaseEndpoint {

    private final RemoteGuildService remoteGuildService;
    private final ShardManager shardManager;

    public GuildEndpoint(RemoteGuildService remoteGuildService, ShardManager shardManager) {
        this.remoteGuildService = remoteGuildService;
        this.shardManager = shardManager;
    }

    protected WebContext assertGuildAccess(WebUser user, long guildId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!user.hasScope(OAuth2Scope.GUILDS)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!this.remoteGuildService.asUser(user).knowsGuild(guildId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Guild guild = getGuild(guildId);
        Member member = getMember(guild, user.getId());

        return new WebContext(user, guild, member);
    }

    protected Guild getGuild(long guildId) {
        Guild guild = this.shardManager.getGuildById(guildId);
        if (guild == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return guild;
    }

    protected Member getMember(Guild guild, long userId) {
        Member member = guild.getMemberById(userId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return member;
    }
}
