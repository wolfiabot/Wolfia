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

package space.npstr.wolfia.webapi;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.db.type.OAuth2Scope;

/**
 * Help verify user access to various resources at the web controller level.
 */
@Component
public class AccessVerifier {

    private static final Logger log = LoggerFactory.getLogger(AccessVerifier.class);

    public boolean hasScope(WebUser user, OAuth2Scope scope) {
        Set<String> scopes = user.accessToken().getScopes();
        if (!scopes.contains(scope.discordName())) {
            log.debug("Missing guilds scope");
            return false;
        }

        return true;
    }
}

