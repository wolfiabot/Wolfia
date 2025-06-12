/*
 * Copyright (C) 2016-2025 the original author or authors
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class GuildSettingsRepositoryTest extends ApplicationTest {

    private static final String DEFAULT_NAME = "Unknown Guild";

    @Autowired
    private GuildSettingsRepository repository;

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_expectDefaultValues() {
        long guildId = uniqueLong();

        var settings = this.repository.findOneOrDefault(guildId);

        assertThat(settings.getGuildId()).isEqualTo(guildId);
        assertThat(settings.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(settings.getIconId()).isEmpty();
    }

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_doNotCreateEntry() {
        long guildId = uniqueLong();

        var settings = this.repository.findOneOrDefault(guildId);

        assertThat(settings.getGuildId()).isEqualTo(guildId);
        var created = this.repository.findOne(guildId);
        assertThat(created).isNull();
    }

    @Test
    void givenExistingEntry_whenFetchingDefault_returnExistingEntry() {
        long guildId = uniqueLong();
        String name = "Wolfia Lounge";

        this.repository.set(guildId, name, null);

        var settings = this.repository.findOneOrDefault(guildId);
        assertThat(settings.getGuildId()).isEqualTo(guildId);
        assertThat(settings.getName()).isEqualTo(name);
        assertThat(settings.getIconId()).isEmpty();
    }
}
