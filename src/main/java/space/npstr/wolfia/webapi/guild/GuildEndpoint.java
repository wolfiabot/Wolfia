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
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.npstr.wolfia.db.type.OAuth2Scope;
import space.npstr.wolfia.domain.guild.GuildInfo;
import space.npstr.wolfia.domain.guild.RemoteGuildService;
import space.npstr.wolfia.webapi.ImmutableUserData;
import space.npstr.wolfia.webapi.UserData;
import space.npstr.wolfia.webapi.user.UserEndpoint;

import static org.springframework.http.ResponseEntity.notFound;

@RestController
@RequestMapping("/api")
public class GuildEndpoint {

    private static final Logger log = LoggerFactory.getLogger(UserEndpoint.class);

    private final OAuth2AuthorizedClientRepository repository;
    private final RemoteGuildService remoteGuildService;

    public GuildEndpoint(OAuth2AuthorizedClientRepository repository, RemoteGuildService remoteGuildService) {
        this.repository = repository;
        this.remoteGuildService = remoteGuildService;
    }

    @GetMapping("/guild/{guildId}")
    public ResponseEntity<GuildInfo> getGuild(HttpServletRequest request, @PathVariable long guildId) {
        Optional<UserData> userDataOpt = identifyUser(request);
        if (userDataOpt.isEmpty()) {
            return nope();
        }

        UserData userData = userDataOpt.get();

        return this.remoteGuildService.asUser(userData)
                .fetchGuild(guildId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> notFound().build());
    }

    @GetMapping("/guilds")
    public ResponseEntity<List<GuildInfo>> getGuilds(HttpServletRequest request) {
        Optional<UserData> userDataOpt = identifyUser(request);
        if (userDataOpt.isEmpty()) {
            return nope();
        }
        UserData userData = userDataOpt.get();
        return ResponseEntity.ok(
                this.remoteGuildService.asUser(userData)
                        .fetchAllGuilds()
        );
    }


    private Optional<UserData> identifyUser(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication instanceof OAuth2AuthenticationToken)
                || !(authentication.getPrincipal() instanceof OAuth2User)) {
            log.debug("Missing authentication or wrong types");
            return Optional.empty();
        }
        OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String userIdStr = (String) principal.getAttributes().get("id");
        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.warn("User id '{}' is not a valid long!", userIdStr);
            return Optional.empty();
        }

        String clientRegistrationId = authenticationToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = repository.loadAuthorizedClient(clientRegistrationId, authentication, request);
        if (client == null) {
            log.debug("Missing OAuth2AuthorizedClient");
            return Optional.empty();
        }
        OAuth2AccessToken accessToken = client.getAccessToken();
        if (accessToken == null) {
            log.debug("Missing OAuth2AccessToken");
            return Optional.empty();
        }

        Set<String> scopes = accessToken.getScopes();
        if (!scopes.contains(OAuth2Scope.GUILDS.discordName())) {
            log.debug("Missing guilds scope");
            return Optional.empty();
        }
        log.debug("{}", scopes.toString());


        return Optional.of(
                ImmutableUserData.builder()
                        .id(userId)
                        .accessToken(accessToken.getTokenValue())
                        .build()
        );
    }

    private <T> ResponseEntity<T> nope() {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
}
