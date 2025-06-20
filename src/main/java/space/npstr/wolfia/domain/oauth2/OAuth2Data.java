/*
 * Copyright (C) 2016-2025 the original author or authors
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

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import space.npstr.wolfia.db.type.OAuth2Scope;

public record OAuth2Data(
        long userId,
        String accessToken,
        Instant expires,
        String refreshToken,
        Set<OAuth2Scope> scopes,
        Instant createdAt
) {

    public OAuth2Data(long userId, String accessToken, Instant expires, String refreshToken, Set<OAuth2Scope> scopes, Instant createdAt) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.expires = expires;
        this.refreshToken = refreshToken;
        this.scopes = EnumSet.copyOf(scopes);
        this.createdAt = createdAt;
    }

    @Override
    public Set<OAuth2Scope> scopes() {
        return Collections.unmodifiableSet(this.scopes);
    }
}
