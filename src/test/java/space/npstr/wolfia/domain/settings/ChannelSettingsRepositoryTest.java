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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;


class ChannelSettingsRepositoryTest extends ApplicationTest {

    private static final int DEFAULT_TAG_COOLDOWN = 5;
    private static final boolean DEFAULT_AUTO_OUT = false;
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
        assertThat(settings.isAutoOut()).isEqualTo(DEFAULT_AUTO_OUT);
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

    @Test
    void givenMixedExistingEntries_whenFetchingDefault_returnRequestedEntries() {
        long channelIdA = uniqueLong();
        long channelIdB = uniqueLong();

        this.repository.setTagCooldown(channelIdA, uniqueLong())
                .toCompletableFuture().join();

        var settingsList = this.repository.findOrDefault(List.of(channelIdA, channelIdB))
                .toCompletableFuture().join();

        assertThat(settingsList)
                .hasSize(2)
                .anySatisfy(channelSettings ->
                        assertThat(channelSettings.getChannelId()).isEqualTo(channelIdA)
                );
        assertThat(settingsList).anySatisfy(channelSettings -> {
            assertThat(channelSettings.getChannelId()).isEqualTo(channelIdB);
        });
        var createdA = this.repository.findOne(channelIdA)
                .toCompletableFuture().join();
        assertThat(createdA).isPresent();
        var createdB = this.repository.findOne(channelIdB)
                .toCompletableFuture().join();
        assertThat(createdB).isEmpty();
    }

}
