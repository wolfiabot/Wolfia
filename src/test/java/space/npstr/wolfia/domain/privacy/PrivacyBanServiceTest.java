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

package space.npstr.wolfia.domain.privacy;

import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.junit.jupiter.api.Test;
import space.npstr.wolfia.App;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.utils.discord.EmptyRestAction;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class PrivacyBanServiceTest extends ApplicationTest {

    @Test
    void whenBanAll_getsBanned() {
        long userId = uniqueLong();
        String userIdStr = Long.toString(userId);
        Guild wolfiaLounge = mock(Guild.class);
        @SuppressWarnings("unchecked") AuditableRestAction<Void> restAction = mock(AuditableRestAction.class);
        when(wolfiaLounge.ban(eq(userIdStr), eq(0), eq("Privacy: Data Processing Denied"))).thenReturn(restAction);
        when(wolfiaLounge.retrieveBanList()).thenReturn(new EmptyRestAction<>(null, List.of()));
        when(shardManager.getGuildById(eq(App.WOLFIA_LOUNGE_ID))).thenReturn(wolfiaLounge);

        privacyBanService.privacyBanAll(List.of(userId));

        //noinspection ResultOfMethodCallIgnored
        verify(wolfiaLounge).ban(eq(userIdStr), eq(0), eq("Privacy: Data Processing Denied"));
    }
}
