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

package space.npstr.wolfia.domain.settings;

import java.util.function.Consumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class GuildCacheListenerTest {

    private final GuildSettingsService service = mock(GuildSettingsService.class);

    private final GuildCacheListener listener = new GuildCacheListener(service);

    @Test
    void onGuildJoin_set() {
        onGuildEvent_set(mock(GuildJoinEvent.class), listener::onGuildJoin);
    }

    @Test
    void onGuildReady_set() {
        onGuildEvent_set(mock(GuildReadyEvent.class), listener::onGuildReady);
    }

    @Test
    void onGuildUpdateIcon_set() {
        onGuildEvent_set(mock(GuildUpdateIconEvent.class), listener::onGuildUpdateIcon);
    }

    @Test
    void onGuildUpdateName_set() {
        onGuildEvent_set(mock(GuildUpdateNameEvent.class), listener::onGuildUpdateName);
    }

    private <T extends GenericGuildEvent> void onGuildEvent_set(T eventMock, Consumer<T> onEvent) {
        long guildId = uniqueLong();
        var guild = mock(Guild.class);
        when(guild.getIdLong()).thenReturn(guildId);
        when(eventMock.getGuild()).thenReturn(guild);
        var jda = mock(JDA.class);
        when(eventMock.getJDA()).thenReturn(jda);

        onEvent.accept(eventMock);

        verify(service).set(eq(guild));
    }

}
