/*
 * Copyright (C) 2016-2023 the original author or authors
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.type.OAuth2Scope;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static space.npstr.wolfia.TestUtil.uniqueLong;
import static space.npstr.wolfia.db.gen.Tables.OAUTH2;

class OAuth2RepositoryTest extends ApplicationTest {

    public static Consumer<OAuth2Data> isOAuth2Data(OAuth2Data data) {
        return actual -> {
            assertThat(actual.userId()).isEqualTo(data.userId());
            assertThat(actual.accessToken()).isEqualTo(data.accessToken());
            assertThat(actual.refreshToken()).isEqualTo(data.refreshToken());
            assertThat(actual.expires()).isCloseTo(data.expires(), within(1, ChronoUnit.MILLIS));
            assertThat(actual.scopes()).containsExactlyInAnyOrderElementsOf(data.scopes());
        };
    }

    @Autowired
    private OAuth2Repository repository;

    @Autowired
    private Database database;

    @BeforeEach
    @AfterEach
    void cleanDbTable() {
        this.database.jooq().transactionResult(config -> config.dsl()
                .deleteFrom(OAUTH2)
                .execute()
        );
    }

    @Test
    void givenNoEntry_whenFind_returnEmpty() {
        long userId = uniqueLong();

        OAuth2Data data = this.repository.findOne(userId);

        assertThat(data).isNull();
    }

    @Test
    void givenEntryPresent_whenFind_returnEntry() {
        long userId = uniqueLong();
        OAuth2Data data = new OAuth2Data(userId, "foo", now().plusDays(30).toInstant(),
                "bar", Set.of(OAuth2Scope.IDENTIFY));

        this.repository.save(data);

        OAuth2Data found = this.repository.findOne(userId);
        assertThat(found).satisfies(isOAuth2Data(data));
    }

    @Test
    void givenNoEntry_whenSave_save() {
        long userId = uniqueLong();
        OAuth2Data data = new OAuth2Data(userId, "foo", now().plusDays(30).toInstant(),
                "bar", Set.of(OAuth2Scope.IDENTIFY));

        OAuth2Data saved = this.repository.save(data);

        OAuth2Data fetched = this.repository.findOne(userId);
        assertThat(saved).satisfies(isOAuth2Data(data));
        assertThat(fetched).satisfies(isOAuth2Data(data));
    }

    @Test
    void givenEntryPresent_whenSave_overwrite() {
        long userId = uniqueLong();
        OAuth2Data existing = new OAuth2Data(userId, "foo", now().plusDays(30).toInstant(),
                "bar", Set.of(OAuth2Scope.IDENTIFY));
        this.repository.save(existing);
        OAuth2Data data = new OAuth2Data(userId, "foo", now().plusDays(14).toInstant(),
                "baz", Set.of(OAuth2Scope.IDENTIFY, OAuth2Scope.GUILD_JOIN));

        OAuth2Data saved = this.repository.save(data);

        OAuth2Data fetched = this.repository.findOne(userId);
        assertThat(saved).satisfies(isOAuth2Data(data));
        assertThat(fetched).satisfies(isOAuth2Data(data));
    }

    @Test
    void givenSoonExpiringEntry_findExpiringEntry() {
        OAuth2Data expiringSoon = expiringOn(now().plusDays(1).toInstant());
        this.repository.save(expiringSoon);

        List<OAuth2Data> expiring = this.repository.findAllExpiringIn(Duration.ofDays(2));

        assertThat(expiring)
                .singleElement()
                .satisfies(isOAuth2Data(expiringSoon));
    }

    @Test
    void givenSoonExpiringEntries_findExpiringEntries() {
        OAuth2Data expiringSoonA = expiringOn(now().plusDays(1).toInstant());
        this.repository.save(expiringSoonA);
        OAuth2Data expiringSoonB = expiringOn(now().plusHours(1).toInstant());
        this.repository.save(expiringSoonB);

        List<OAuth2Data> expiring = this.repository.findAllExpiringIn(Duration.ofDays(2));

        assertThat(expiring).hasSize(2);
        assertThat(expiring).filteredOnAssertions(isOAuth2Data(expiringSoonA)).hasSize(1);
        assertThat(expiring).filteredOnAssertions(isOAuth2Data(expiringSoonB)).hasSize(1);
    }

    @Test
    void givenABunchOfExpiringEntries_findOnlyExpiringEntries() {
        OAuth2Data expiringSoonA = expiringOn(now().plusDays(1).toInstant());
        this.repository.save(expiringSoonA);
        OAuth2Data expiringSoonB = expiringOn(now().plusHours(1).toInstant());
        this.repository.save(expiringSoonB);
        OAuth2Data notExpiringSoonA = expiringOn(now().plusWeeks(3).toInstant());
        this.repository.save(notExpiringSoonA);
        OAuth2Data notExpiringSoonB = expiringOn(now().plusMonths(1).toInstant());
        this.repository.save(notExpiringSoonB);

        List<OAuth2Data> expiring = this.repository.findAllExpiringIn(Duration.ofDays(2));

        assertThat(expiring).hasSize(2);
        assertThat(expiring).filteredOnAssertions(isOAuth2Data(expiringSoonA)).hasSize(1);
        assertThat(expiring).filteredOnAssertions(isOAuth2Data(expiringSoonB)).hasSize(1);
    }

    @Test
    void givenNoEntry_whenDelete_noRowsChanged() {
        int deleted = this.repository.delete(uniqueLong());

        assertThat(deleted).isZero();
    }

    @Test
    void whenDelete_delete() {
        long userId = uniqueLong();
        OAuth2Data existing = new OAuth2Data(userId, "foo", now().plusDays(30).toInstant(),
                "bar", Set.of(OAuth2Scope.IDENTIFY));
        this.repository.save(existing);

        int deleted = this.repository.delete(userId);

        OAuth2Data fetched = this.repository.findOne(userId);
        assertThat(deleted).isOne();
        assertThat(fetched).isNull();
    }

    private OAuth2Data expiringOn(Instant expiringOn) {
        return new OAuth2Data(uniqueLong(), "foo", expiringOn,
                "bar", EnumSet.allOf(OAuth2Scope.class));
    }

}
