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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class OAuth2RepositoryTest extends ApplicationTest {

    public static Consumer<OAuth2Data> isOAuth2Data(OAuth2Data data) {
        return actual -> {
            assertThat(actual.userId()).isEqualTo(data.userId());
            assertThat(actual.accessToken()).isEqualTo(data.accessToken());
            assertThat(actual.refreshToken()).isEqualTo(data.refreshToken());
            assertThat(actual.expires()).isEqualTo(data.expires());
            assertThat(actual.scopes()).containsExactlyInAnyOrderElementsOf(data.scopes());
        };
    }

    @Autowired
    private OAuth2Repository repository;

    @Test
    void givenNoEntry_whenFind_returnEmpty() {
        long userId = uniqueLong();

        Optional<OAuth2Data> data = this.repository.findOne(userId).toCompletableFuture().join();

        assertThat(data).isEmpty();
    }

    @Test
    void givenEntryPresent_whenFind_returnEntry() {
        long userId = uniqueLong();

        OAuth2Data data = new OAuth2Data(userId, "foo", OffsetDateTime.now().plusDays(30),
                "bar", Set.of(OAuth2Scope.IDENTIFY));
        this.repository.save(data).toCompletableFuture().join();
        Optional<OAuth2Data> dataOpt = this.repository.findOne(userId).toCompletableFuture().join();

        assertThat(dataOpt).hasValueSatisfying(isOAuth2Data(data));
    }

    @Test
    void givenNoEntry_whenSave_save() {
        long userId = uniqueLong();

        OAuth2Data data = new OAuth2Data(userId, "foo", OffsetDateTime.now().plusDays(30),
                "bar", Set.of(OAuth2Scope.IDENTIFY));
        OAuth2Data saved = this.repository.save(data).toCompletableFuture().join();
        Optional<OAuth2Data> fetched = this.repository.findOne(userId).toCompletableFuture().join();

        assertThat(saved).satisfies(isOAuth2Data(data));
        assertThat(fetched).hasValueSatisfying(isOAuth2Data(data));
    }

    @Test
    void givenEntryPresent_whenSave_overwrite() {
        long userId = uniqueLong();
        OAuth2Data existing = new OAuth2Data(userId, "foo", OffsetDateTime.now().plusDays(30),
                "bar", Set.of(OAuth2Scope.IDENTIFY));
        this.repository.save(existing).toCompletableFuture().join();

        OAuth2Data data = new OAuth2Data(userId, "foo", OffsetDateTime.now().plusDays(14),
                "baz", Set.of(OAuth2Scope.IDENTIFY, OAuth2Scope.GUILD_JOIN));
        OAuth2Data saved = this.repository.save(data).toCompletableFuture().join();
        Optional<OAuth2Data> fetched = this.repository.findOne(userId).toCompletableFuture().join();

        assertThat(saved).satisfies(isOAuth2Data(data));
        assertThat(fetched).hasValueSatisfying(isOAuth2Data(data));
    }

}
