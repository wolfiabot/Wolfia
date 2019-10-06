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
import space.npstr.wolfia.db.gen.tables.records.ChannelSettingsRecord;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


class ChannelSettingsRepositoryTest extends ApplicationTest {

    // tied to default values of the db
    private static final int DEFAULT_ACCESS_ROLE_ID = -1;
    private static final int DEFAULT_TAG_COOLDOWN = 5;
    private static final int DEFAULT_TAG_LAST_USED = 0;

    @Autowired
    private ChannelSettingsRepository repository;

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_expectDefaultValues() {
        long channelId = uniqueLong();

        ChannelSettingsRecord channelSettings = this.repository.findOneOrDefault(channelId)
                .toCompletableFuture().join();

        assertThat(channelSettings.getChannelId()).isEqualTo(channelId);
        assertThat(channelSettings.getAccessRoleId()).isEqualTo(DEFAULT_ACCESS_ROLE_ID);
        assertThat(channelSettings.getTagCooldown()).isEqualTo(DEFAULT_TAG_COOLDOWN);
        assertThat(channelSettings.getTagLastUsed()).isEqualTo(DEFAULT_TAG_LAST_USED);
        assertThat(channelSettings.getTags()).isEmpty();
    }

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_doNotCreateEntry() {
        long channelId = uniqueLong();

        ChannelSettingsRecord channelSettings = this.repository.findOneOrDefault(channelId)
                .toCompletableFuture().join();

        assertThat(channelSettings.getChannelId()).isEqualTo(channelId);
        Optional<ChannelSettingsRecord> created = this.repository.findOne(channelId)
                .toCompletableFuture().join();
        assertThat(created.isPresent()).isFalse();
    }

    @Test
    void givenExistingEntry_whenFetchingDefault_returnExistingEntry() {
        long channelId = uniqueLong();
        long tagCooldown = uniqueLong();

        this.repository.setTagCooldown(channelId, tagCooldown)
                .toCompletableFuture().join();

        ChannelSettingsRecord channelSettings = this.repository.findOneOrDefault(channelId)
                .toCompletableFuture().join();

        assertThat(channelSettings.getChannelId()).isEqualTo(channelId);
        assertThat(channelSettings.getTagCooldown()).isEqualTo(tagCooldown);
    }

}
