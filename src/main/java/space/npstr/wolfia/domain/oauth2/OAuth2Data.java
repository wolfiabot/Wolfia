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

package space.npstr.wolfia.domain.oauth2;

import space.npstr.wolfia.db.type.OAuth2Scope;

import java.beans.ConstructorProperties;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class OAuth2Data {

    private final long userId;
    private final String accessToken;
    private final Instant expires;
    private final String refreshToken;
    private final Set<OAuth2Scope> scopes;

    @ConstructorProperties({"userId", "accessToken", "expires", "refreshToken", "scopes"})
    public OAuth2Data(long userId, String accessToken, Instant expires, String refreshToken, Set<OAuth2Scope> scopes) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.expires = expires;
        this.refreshToken = refreshToken;
        this.scopes = EnumSet.copyOf(scopes);
    }

    public long userId() {
        return userId;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public String accessToken() {
        return accessToken;
    }

    public Instant expires() {
        return expires;
    }

    public Set<OAuth2Scope> scopes() {
        return Collections.unmodifiableSet(this.scopes);
    }
}
