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

package space.npstr.wolfia.domain.ban;


import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.AsyncDbWrapper;
import space.npstr.wolfia.db.gen.tables.records.BanRecord;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.db.gen.Tables.BAN;

class BanServiceTest extends ApplicationTest {

    @Autowired
    private BanService banService;

    @Autowired
    private AsyncDbWrapper wrapper;


    @BeforeEach
    void setup() {
        this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .deleteFrom(BAN)
                .execute()
        )).toCompletableFuture().join();
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
        List<BanRecord> bans = this.banService.getActiveBans();

        assertThat(bans).isEmpty();
    }

    @Test
    void givenOneBan_whenGetAllBansCalled_returnOneBan() {
        long userId = uniqueLong();
        this.banService.ban(userId);

        var bans = this.banService.getActiveBans();

        assertThat(bans).size().isEqualTo(1);
        assertThat(bans).hasOnlyOneElementSatisfying(isUser(userId));
    }

    @Test
    void givenMultipleBans_whenGetAllBansCalled_returnMultipleBans() {
        long userIdA = uniqueLong();
        long userIdB = uniqueLong();

        this.banService.ban(userIdA);
        this.banService.ban(userIdB);

        var bans = this.banService.getActiveBans();

        assertThat(bans).size().isEqualTo(2);
        assertThat(bans).filteredOnAssertions(isUser(userIdA)).size().isEqualTo(1);
        assertThat(bans).filteredOnAssertions(isUser(userIdB)).size().isEqualTo(1);
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

        assertThat(bans).size().isEqualTo(2);
        assertThat(bans).filteredOnAssertions(isUser(userIdA)).size().isEqualTo(1);
        assertThat(bans).filteredOnAssertions(isUser(userIdB)).size().isEqualTo(1);
    }

    private Consumer<BanRecord> isUser(long userId) {
        return ban -> assertThat(ban.getUserId()).isEqualTo(userId);
    }
}
