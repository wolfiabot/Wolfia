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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.type.OAuth2Scope;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static java.time.OffsetDateTime.now;
import static java.util.concurrent.CompletableFuture.completedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class OAuth2ServiceTest extends ApplicationTest {

    @Autowired
    private OAuth2Service service;
    @Autowired
    private OAuth2Repository repository;

    @Test
    void whenAcceptCode_requestFromDiscordAndSave() {
        long userId = uniqueLong();
        String accessToken = "foo";
        Instant expires = now().plusDays(14).toInstant();
        String refreshToken = "bar";
        Set<OAuth2Scope> scopes = EnumSet.allOf(OAuth2Scope.class);
        ImmutableAccessTokenResponse codeResponse = ImmutableAccessTokenResponse.builder()
                .accessToken(accessToken)
                .expires(expires)
                .refreshToken(refreshToken)
                .addAllScopes(scopes)
                .build();
        when(oAuth2Requester.fetchCodeResponse(any())).thenReturn(completedStage(codeResponse));
        when(oAuth2Requester.identifyUser(any())).thenReturn(completedStage(userId));

        this.service.acceptCode("foo").toCompletableFuture().join();

        Optional<OAuth2Data> oAuth2Data = repository.findOne(userId).toCompletableFuture().join();
        assertThat(oAuth2Data).hasValueSatisfying(actual -> {
            assertThat(actual.userId()).isEqualTo(userId);
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.expires()).isEqualTo(expires);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
            assertThat(actual.scopes()).containsExactlyInAnyOrderElementsOf(scopes);
        });
    }

    @Test
    void givenNoAccessToken_whenGetAccessTokenForScope_returnEmpty() {
        Optional<String> fetched = this.service.getAccessTokenForScope(uniqueLong(), OAuth2Scope.IDENTIFY);

        assertThat(fetched).isEmpty();
    }

    @Test
    void givenAccessTokenExists_whenGetAccessTokenForScope_returnAccessToken() {
        long userId = uniqueLong();
        String accessToken = "foo";
        OAuth2Data validOAuth2Data = new OAuth2Data(userId, accessToken, now().plusDays(14).toInstant(),
                "bar", EnumSet.allOf(OAuth2Scope.class));
        this.repository.save(validOAuth2Data).toCompletableFuture().join();

        Optional<String> fetched = this.service.getAccessTokenForScope(userId, OAuth2Scope.IDENTIFY);

        assertThat(fetched).hasValue(accessToken);
    }

    @Test
    void givenAccessTokenExistsWithWrongScope_whenGetAccessTokenForScope_returnEmpty() {
        long userId = uniqueLong();
        String accessToken = "foo";
        OAuth2Data validOAuth2Data = new OAuth2Data(userId, accessToken, now().plusDays(14).toInstant(),
                "bar", EnumSet.of(OAuth2Scope.IDENTIFY));
        this.repository.save(validOAuth2Data).toCompletableFuture().join();

        Optional<String> fetched = this.service.getAccessTokenForScope(userId, OAuth2Scope.GUILD_JOIN);

        assertThat(fetched).isEmpty();
    }

    @Test
    void givenAccessTokenExistsOutdated_whenGetAccessTokenForScope_returnEmpty() {
        long userId = uniqueLong();
        String accessToken = "foo";
        OAuth2Data validOAuth2Data = new OAuth2Data(userId, accessToken, now().minusDays(1).toInstant(),
                "bar", EnumSet.allOf(OAuth2Scope.class));
        this.repository.save(validOAuth2Data).toCompletableFuture().join();

        Optional<String> fetched = this.service.getAccessTokenForScope(userId, OAuth2Scope.IDENTIFY);

        assertThat(fetched).isEmpty();
    }

}
