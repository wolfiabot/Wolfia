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

import net.dv8tion.jda.api.entities.Guild;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class GuildSettingsRepositoryTest extends ApplicationTest {

    private static final String DEFAULT_NAME = "Unknown Guild";

    @Autowired
    private GuildSettingsRepository repository;

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_expectDefaultValues() {
        long guildId = uniqueLong();

        var settings = this.repository.findOneOrDefault(guildId)
                .toCompletableFuture().join();

        assertThat(settings.getGuildId()).isEqualTo(guildId);
        assertThat(settings.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(settings.getIconId()).isEmpty();
    }

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_doNotCreateEntry() {
        long guildId = uniqueLong();

        var settings = this.repository.findOneOrDefault(guildId)
                .toCompletableFuture().join();

        assertThat(settings.getGuildId()).isEqualTo(guildId);
        var created = this.repository.findOne(guildId)
                .toCompletableFuture().join();
        assertThat(created.isPresent()).isFalse();
    }

    @Test
    void givenExistingEntry_whenFetchingDefault_returnExistingEntry() {
        long guildId = uniqueLong();
        String name = "Wolfia Lounge";

        Guild guild = mock(Guild.class);
        doReturn(guildId).when(guild).getIdLong();
        doReturn(name).when(guild).getName();

        this.repository.set(guildId, name, null)
                .toCompletableFuture().join();

        var settings = this.repository.findOneOrDefault(guildId)
                .toCompletableFuture().join();

        assertThat(settings.getGuildId()).isEqualTo(guildId);
        assertThat(settings.getName()).isEqualTo(name);
        assertThat(settings.getIconId()).isEmpty();
    }
}
