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

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * User that is doing the web request
 */
@Immutable
@Value.Style(
        stagedBuilder = true,
        strictBuilder = true
)
public interface WebUser {

    /**
     * discord id of the user
     */
    long id();

    /**
     * Principal user object with further
     */
    OAuth2User principal();

    /**
     * Oauth2 access token
     */
    OAuth2AccessToken accessToken();
}
