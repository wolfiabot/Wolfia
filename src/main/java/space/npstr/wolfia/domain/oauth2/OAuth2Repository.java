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

package space.npstr.wolfia.domain.oauth2;

import org.jooq.Converter;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.gen.enums.Oauth2scope;
import space.npstr.wolfia.db.type.OAuth2Scope;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static space.npstr.wolfia.db.gen.Tables.OAUTH2;

@Repository
public class OAuth2Repository {

    private final AsyncDbWrapper wrapper;

    public OAuth2Repository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public CompletionStage<Optional<OAuth2Data>> findOne(long userId) {
        return this.wrapper.jooq(dsl -> dsl
                .select(OAUTH2.USER_ID, OAUTH2.ACCESS_TOKEN, OAUTH2.EXPIRES, OAUTH2.REFRESH_TOKEN, convertedScopesField())
                .from(OAUTH2)
                .where(OAUTH2.USER_ID.eq(userId))
                .fetchOptionalInto(OAuth2Data.class)
        );
    }

    public CompletionStage<List<OAuth2Data>> findAllExpiringIn(Duration duration) {
        OffsetDateTime expiresOn = OffsetDateTime.now().plusSeconds(duration.toSeconds());
        return this.wrapper.jooq(dsl -> dsl
                .select(OAUTH2.USER_ID, OAUTH2.ACCESS_TOKEN, OAUTH2.EXPIRES, OAUTH2.REFRESH_TOKEN, convertedScopesField())
                .from(OAUTH2)
                .where(OAUTH2.EXPIRES.lessThan(expiresOn))
                .fetchInto(OAuth2Data.class)
        );
    }

    public CompletionStage<Integer> delete(long userId) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .deleteFrom(OAUTH2)
                .where(OAUTH2.USER_ID.eq(userId))
                .execute()
        ));
    }

    public CompletionStage<OAuth2Data> save(OAuth2Data data) {
        // Need a conversion until https://github.com/jOOQ/jOOQ/issues/9523 is fixed
        Oauth2scope[] scopes = data.scopes().stream()
                .map(OAuth2Scope::name)
                .map(Oauth2scope::valueOf)
                .collect(Collectors.toSet())
                .toArray(new Oauth2scope[]{});

        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(OAUTH2)
                .columns(OAUTH2.USER_ID, OAUTH2.ACCESS_TOKEN, OAUTH2.EXPIRES, OAUTH2.REFRESH_TOKEN, OAUTH2.SCOPES)
                .values(data.userId(), data.accessToken(), data.expires(), data.refreshToken(), scopes)
                .onDuplicateKeyUpdate()
                .set(OAUTH2.ACCESS_TOKEN, data.accessToken())
                .set(OAUTH2.EXPIRES, data.expires())
                .set(OAUTH2.REFRESH_TOKEN, data.refreshToken())
                .set(OAUTH2.SCOPES, scopes)
                .returningResult(OAUTH2.USER_ID, OAUTH2.ACCESS_TOKEN, OAUTH2.EXPIRES, OAUTH2.REFRESH_TOKEN, convertedScopesField())
                .fetchOne()
                .into(OAuth2Data.class)
        ));
    }

    // Need a conversion until https://github.com/jOOQ/jOOQ/issues/9523 is fixed
    private Field<OAuth2Scope[]> convertedScopesField() {
        return DSL.field(OAUTH2.SCOPES.getQualifiedName(), OAUTH2.SCOPES.getDataType().asConvertedDataType(scopesFieldConverter()));
    }

    private Converter<Oauth2scope[], OAuth2Scope[]> scopesFieldConverter() {
        return Converter.ofNullable(
                Oauth2scope[].class,
                OAuth2Scope[].class,
                dbScopes -> Arrays.stream(dbScopes)
                        .map(Oauth2scope::name)
                        .map(OAuth2Scope::valueOf)
                        .collect(Collectors.toSet())
                        .toArray(new OAuth2Scope[]{}),
                wolfiaScopes -> Arrays.stream(wolfiaScopes)
                        .map(OAuth2Scope::name)
                        .map(Oauth2scope::valueOf)
                        .collect(Collectors.toSet())
                        .toArray(new Oauth2scope[]{})
        );
    }
}
