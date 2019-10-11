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
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;


class ChannelSettingsRepositoryTest extends ApplicationTest {

    private static final int DEFAULT_TAG_COOLDOWN = 5;
    private static final int DEFAULT_TAG_LAST_USED = 0;

    @Autowired
    private ChannelSettingsRepository repository;

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_expectDefaultValues() {
        long channelId = uniqueLong();

        var settings = this.repository.findOneOrDefault(channelId)
                .toCompletableFuture().join();

        assertThat(settings.getChannelId()).isEqualTo(channelId);
        assertThat(settings.getAccessRoleId()).isEmpty();
        assertThat(settings.getTagCooldownMinutes()).isEqualTo(DEFAULT_TAG_COOLDOWN);
        assertThat(settings.getTagLastUsed()).isEqualTo(DEFAULT_TAG_LAST_USED);
        assertThat(settings.getTags()).isEmpty();
    }

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_doNotCreateEntry() {
        long channelId = uniqueLong();

        var settings = this.repository.findOneOrDefault(channelId)
                .toCompletableFuture().join();

        assertThat(settings.getChannelId()).isEqualTo(channelId);
        var created = this.repository.findOne(channelId)
                .toCompletableFuture().join();
        assertThat(created.isPresent()).isFalse();
    }

    @Test
    void givenExistingEntry_whenFetchingDefault_returnExistingEntry() {
        long channelId = uniqueLong();
        long tagCooldown = uniqueLong();

        this.repository.setTagCooldown(channelId, tagCooldown)
                .toCompletableFuture().join();

        var settings = this.repository.findOneOrDefault(channelId)
                .toCompletableFuture().join();

        assertThat(settings.getChannelId()).isEqualTo(channelId);
        assertThat(settings.getTagCooldownMinutes()).isEqualTo(tagCooldown);
    }

}
