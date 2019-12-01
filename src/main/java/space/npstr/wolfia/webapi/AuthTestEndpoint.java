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

package space.npstr.wolfia.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test")
public class AuthTestEndpoint {

    private static final Logger log = LoggerFactory.getLogger(AuthTestEndpoint.class);

    @GetMapping("name")
    public String getName(Authentication authentication) {
        String name = "mysterious user";

        if (authentication != null) {
            Object rawPrincipal = authentication.getPrincipal();
            if (rawPrincipal instanceof OAuth2User) {
                OAuth2User principal = (OAuth2User) rawPrincipal;
                name = principal.getName();
                String attributes = principal.getAttributes().entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining(", "));
                String authorities = principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(", "));
                log.info("User {} with attributes [{}] & authorities [{}]", name, attributes, authorities);
            }
        }

        return name;
    }
}
