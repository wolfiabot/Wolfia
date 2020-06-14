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

package space.npstr.wolfia.webapi.user;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.npstr.wolfia.db.type.OAuth2Scope;
import space.npstr.wolfia.webapi.BaseEndpoint;
import space.npstr.wolfia.webapi.WebUser;

@RestController
@RequestMapping("/public/user")
public class UserEndpoint extends BaseEndpoint {

    @GetMapping
    public ResponseEntity<SelfUser> getSelf(@Nullable WebUser user) {
        if (user == null || !user.hasScope(OAuth2Scope.IDENTIFY)) {
            return unauthorized();
        }

        OAuth2User principal = user.principal();
        Map<String, Object> attributes = principal.getAttributes();
        String userId = (String) attributes.get("id");
        String discriminator = (String) attributes.get("discriminator");
        String avatar = (String) attributes.get("avatar");

        Set<String> roles = filterAndCollectByPrefix(principal.getAuthorities(), "ROLE_");
        Set<String> scopes = filterAndCollectByPrefix(principal.getAuthorities(), "SCOPE_");

        SelfUser selfUser = ImmutableSelfUser.builder()
                .discordId(userId)
                .name(principal.getName())
                .discriminator(discriminator)
                .avatarId(Optional.ofNullable(avatar))
                .addAllRoles(roles)
                .addAllScopes(scopes)
                .build();

        return ResponseEntity.ok(selfUser);
    }

    private Set<String> filterAndCollectByPrefix(Collection<? extends GrantedAuthority> authorities, String prefix) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith(prefix))
                .map(auth -> auth.substring(prefix.length()))
                .collect(Collectors.toSet());
    }
}
