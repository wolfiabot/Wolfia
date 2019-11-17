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

package space.npstr.wolfia.db.converter;

import org.jooq.Converter;
import space.npstr.wolfia.db.gen.enums.Oauth2scope;
import space.npstr.wolfia.db.type.OAuth2Scope;

import java.util.Arrays;
import java.util.stream.Collectors;

public class OAuth2ScopeEnumArrayConverter implements Converter<Oauth2scope[], OAuth2Scope[]> {

    @Override
    public OAuth2Scope[] from(Oauth2scope[] databaseObject) {
        return Arrays.stream(databaseObject)
                .map(s -> OAuth2Scope.parse(s.name()).orElseThrow(() ->
                        new RuntimeException("Unknown OAuth2Scope " + s.name())))
                .collect(Collectors.toSet())
                .toArray(new OAuth2Scope[]{});
    }

    @Override
    public Oauth2scope[] to(OAuth2Scope[] userObject) {
        return Arrays.stream(userObject)
                .map(s -> Oauth2scope.valueOf(s.name()))
                .collect(Collectors.toSet())
                .toArray(new Oauth2scope[]{});
    }

    @Override
    public Class<Oauth2scope[]> fromType() {
        return Oauth2scope[].class;
    }

    @Override
    public Class<OAuth2Scope[]> toType() {
        return OAuth2Scope[].class;
    }
}
