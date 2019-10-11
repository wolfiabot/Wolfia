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

import org.junit.jupiter.api.Test;
import space.npstr.wolfia.ApplicationTest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GuildSettingsTest {

    @Test
    void whenIconIdIsNull_avatarUrlShouldBeNull() {
        var settings = new GuildSettings(ApplicationTest.uniqueLong(), "Wolfia Lounge", null);

        assertThat(settings.getAvatarUrl()).isEmpty();
    }

    @Test
    void whenIconIdIsNotNull_avatarUrlShouldNotBeNull() {
        var settings = new GuildSettings(ApplicationTest.uniqueLong(), "Wolfia Lounge", "424242");

        assertThat(settings.getAvatarUrl()).isPresent()
                .hasValueSatisfying(avatarUrl -> assertThat(avatarUrl).isNotBlank());
    }

    @Test
    void avatarUrlIsSensibleUrl() {
        long guilId = ApplicationTest.uniqueLong();

        var settings = new GuildSettings(guilId, "Wolfia Lounge", "424242");

        String avatarUrl = settings.getAvatarUrl().orElseThrow();
        assertThatCode(() -> new URL(avatarUrl).toURI()).doesNotThrowAnyException();
    }

}
