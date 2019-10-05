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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        assertTrue(this.banService.isBanned(userId));
    }

    @Test
    void whenUnbanned_shouldNotBeBanned() {
        long userId = uniqueLong();

        this.banService.unban(userId);

        assertFalse(this.banService.isBanned(userId));
    }

    @Test
    void givenNoBans_whenGetAllBansCalled_returnNoBans() {
        List<BanRecord> bans = this.banService.getActiveBans();

        assertEquals(0, bans.size());
    }

    @Test
    void givenOneBan_whenGetAllBansCalled_returnOneBan() {
        long userId = uniqueLong();
        this.banService.ban(userId);

        List<BanRecord> bans = this.banService.getActiveBans();

        assertEquals(1, bans.size());
        assertEquals(userId, bans.get(0).getUserId());
    }

    @Test
    void givenMultipleBans_whenGetAllBansCalled_returnMultipleBans() {
        long userIdA = uniqueLong();
        long userIdB = uniqueLong();

        this.banService.ban(userIdA);
        this.banService.ban(userIdB);

        List<BanRecord> bans = this.banService.getActiveBans();

        assertEquals(2, bans.size());
        assertTrue(bans.stream().anyMatch(ban -> ban.getUserId() == userIdA));
        assertTrue(bans.stream().anyMatch(ban -> ban.getUserId() == userIdB));
    }

    @Test
    void givenMultipleBansAndUnbans_whenGetAllBansCalled_returnOnlyBans() {
        long userIdA = uniqueLong();
        long userIdB = uniqueLong();
        long userIdC = uniqueLong();


        this.banService.ban(userIdA);
        this.banService.ban(userIdB);
        this.banService.unban(userIdC);


        List<BanRecord> bans = this.banService.getActiveBans();

        assertEquals(2, bans.size());
        assertTrue(bans.stream().anyMatch(ban -> ban.getUserId() == userIdA));
        assertTrue(bans.stream().anyMatch(ban -> ban.getUserId() == userIdB));
    }
}
