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

package space.npstr.wolfia.domain.ban;


import java.util.List;
import java.util.function.Consumer;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;
import static space.npstr.wolfia.db.gen.Tables.DISCORD_USER;

class BanServiceTest extends ApplicationTest {

    @Autowired
    private BanService banService;

    @Autowired
    private DSLContext jooq;


    @BeforeEach
    void setup() {
        this.jooq.transactionResult(config -> config.dsl()
                .deleteFrom(DISCORD_USER)
                .execute()
        );
    }


    @Test
    void whenBanned_shouldBeBanned() {
        long userId = uniqueLong();

        this.banService.ban(userId);

        assertThat(this.banService.isBanned(userId)).isTrue();
    }

    @Test
    void whenUnbanned_shouldNotBeBanned() {
        long userId = uniqueLong();

        this.banService.unban(userId);

        assertThat(this.banService.isBanned(userId)).isFalse();
    }

    @Test
    void givenNoBans_whenGetAllBansCalled_returnNoBans() {
        List<Ban> bans = this.banService.getActiveBans();

        assertThat(bans).isEmpty();
    }

    @Test
    void givenOneBan_whenGetAllBansCalled_returnOneBan() {
        long userId = uniqueLong();
        this.banService.ban(userId);

        var bans = this.banService.getActiveBans();

        assertThat(bans)
                .singleElement()
                .satisfies(isUser(userId));
    }

    @Test
    void givenMultipleBans_whenGetAllBansCalled_returnMultipleBans() {
        long userIdA = uniqueLong();
        long userIdB = uniqueLong();

        this.banService.ban(userIdA);
        this.banService.ban(userIdB);

        var bans = this.banService.getActiveBans();

        assertThat(bans).hasSize(2);
        assertThat(bans).filteredOnAssertions(isUser(userIdA)).hasSize(1);
        assertThat(bans).filteredOnAssertions(isUser(userIdB)).hasSize(1);
    }

    @Test
    void givenMultipleBansAndUnbans_whenGetAllBansCalled_returnOnlyBans() {
        long userIdA = uniqueLong();
        long userIdB = uniqueLong();
        long userIdC = uniqueLong();


        this.banService.ban(userIdA);
        this.banService.ban(userIdB);
        this.banService.unban(userIdC);


        var bans = this.banService.getActiveBans();

        assertThat(bans).hasSize(2);
        assertThat(bans).filteredOnAssertions(isUser(userIdA)).hasSize(1);
        assertThat(bans).filteredOnAssertions(isUser(userIdB)).hasSize(1);
    }

    private Consumer<Ban> isUser(long userId) {
        return ban -> assertThat(ban.getUserId()).isEqualTo(userId);
    }
}
