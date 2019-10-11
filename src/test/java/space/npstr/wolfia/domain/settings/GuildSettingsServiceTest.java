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

package space.npstr.wolfia.domain.settings;

import net.dv8tion.jda.core.entities.Guild;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class GuildSettingsServiceTest extends ApplicationTest {

    @Autowired
    private GuildSettingsService service;

    @Autowired
    private GuildSettingsRepository repository;

    @Test
    void whenGetting_correctSettingsIsReturned() {
        long guildId = uniqueLong();

        var settings = this.service.guild(guildId).getOrDefault();

        assertThat(settings.getGuildId()).isEqualTo(guildId);
    }

    @Test
    void whenSettingGuild_nameIsSaved() {
        long guildId = uniqueLong();
        String name = "Wolfia Lounge";

        Guild guild = mock(Guild.class);
        doReturn(guildId).when(guild).getIdLong();
        doReturn(name).when(guild).getName();

        this.service.set(guild);

        var settings = this.repository.findOne(guildId).toCompletableFuture().join().orElseThrow();
        assertThat(settings.getName()).isEqualTo(name);
    }

    @Test
    void whenSettingGuild_iconIdIsSaved() {
        long guildId = uniqueLong();
        String name = "Wolfia Lounge";
        String iconId = "424242";

        Guild guild = mock(Guild.class);
        doReturn(guildId).when(guild).getIdLong();
        doReturn(name).when(guild).getName();
        doReturn(iconId).when(guild).getIconId();

        this.service.set(guild);

        var settings = this.repository.findOne(guildId).toCompletableFuture().join().orElseThrow();
        assertThat(settings.getIconId()).hasValue(iconId);
    }

}
