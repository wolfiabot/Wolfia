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

package space.npstr.wolfia.webapi;

import java.util.Optional;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.HttpUrl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.npstr.wolfia.App;

/**
 * This endpoint is meant to replace all the various copy&pasted invite links out there.
 * <p>
 * The bonus is that we can generate it on the fly with dynamic parts, like a preselected guild.
 */
@RestController
@RequestMapping("/invite")
public class InviteEndpoint {

    private final ShardManager shardManager;

    public InviteEndpoint(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    /**
     * Redirect should look something like
     * https://discord.com/oauth2/authorize?client_id=306583221565521921&scope=bot&permissions=268787777&response_type=code&redirect_uri=https%3A%2F%2Fdiscord.gg%2FnvcfX3q
     */
    @GetMapping
    public ResponseEntity<Void> redirectToInvite(
            @RequestParam(required = false, name = "guild_id") Optional<String> guildId,
            @RequestParam(required = false, name = "redirect_uri") Optional<String> redirectUri
    ) {
        HttpUrl.Builder inviteUrlBuilder = new HttpUrl.Builder()
                .scheme("https")
                .host("discord.com")
                .addPathSegment("oauth2")
                .addPathSegment("authorize")
                .addQueryParameter("client_id", Long.toString(getBotId()))
                .addQueryParameter("scope", "bot applications.commands")
                .addQueryParameter("permissions", Long.toString(getPermissions()))
                // This is a hack that will redirect the user after the invite to the Wolfia Lounge.
                // It is probably a bad idea to redirect to URLs we don't trust as they might receive OAuth2 access tokens of the user.
                .addQueryParameter("response_type", "code")
                .addQueryParameter("redirect_uri", redirectUri.orElse(App.WOLFIA_LOUNGE_INVITE));

        guildId.ifPresent(s -> inviteUrlBuilder.addQueryParameter("guild_id", s));

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(inviteUrlBuilder.build().uri());
        return new ResponseEntity<>(null, headers, HttpStatus.TEMPORARY_REDIRECT);
    }

    private long getPermissions() {
        return Permission.MANAGE_ROLES.getRawValue()
                | Permission.CREATE_INSTANT_INVITE.getRawValue()
                | Permission.MESSAGE_MANAGE.getRawValue()
                | Permission.MESSAGE_EMBED_LINKS.getRawValue()
                | Permission.MESSAGE_HISTORY.getRawValue()
                | Permission.MESSAGE_ADD_REACTION.getRawValue()
                | Permission.MESSAGE_EXT_EMOJI.getRawValue();
    }

    private long getBotId() {
        return this.shardManager.getShardCache().stream()
                .<Optional<SelfUser>>map(shard -> {
                    try {
                        return Optional.of(shard.getSelfUser());
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ISnowflake::getIdLong)
                .findAny()
                .orElseThrow();
    }

}
