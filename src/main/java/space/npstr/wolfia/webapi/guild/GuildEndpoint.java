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

package space.npstr.wolfia.webapi.guild;

import java.util.List;
import javax.annotation.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.npstr.wolfia.db.type.OAuth2Scope;
import space.npstr.wolfia.domain.guild.GuildInfo;
import space.npstr.wolfia.domain.guild.RemoteGuildService;
import space.npstr.wolfia.webapi.BaseEndpoint;
import space.npstr.wolfia.webapi.WebUser;

import static org.springframework.http.ResponseEntity.notFound;

@RestController
@RequestMapping("/api")
public class GuildEndpoint extends BaseEndpoint {

    private final RemoteGuildService remoteGuildService;

    public GuildEndpoint(RemoteGuildService remoteGuildService) {
        this.remoteGuildService = remoteGuildService;
    }

    @GetMapping("/guild/{guildId}")
    public ResponseEntity<GuildInfo> getGuild(@PathVariable long guildId, @Nullable WebUser user) {
        if (user == null) {
            return unauthorized();
        }
        if (!user.hasScope(OAuth2Scope.GUILDS)) {
            return unauthorized();
        }

        return this.remoteGuildService.asUser(user)
                .fetchGuild(guildId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> notFound().build());
    }

    @GetMapping("/guilds")
    public ResponseEntity<List<GuildInfo>> getGuilds(@Nullable WebUser user) {
        if (user == null || !user.hasScope(OAuth2Scope.GUILDS)) {
            return unauthorized();
        }

        return ResponseEntity.ok(
                this.remoteGuildService.asUser(user)
                        .fetchAllGuilds()
        );
    }
}
