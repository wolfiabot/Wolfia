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

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static space.npstr.wolfia.TestUtil.uniqueLong;


class ChannelSettingsServiceTest extends ApplicationTest {

    @Autowired
    private ChannelSettingsService service;

    @Autowired
    private ChannelSettingsRepository repository;

    @Test
    void whenGetting_correctSettingsIsReturned() {
        long channelId = uniqueLong();

        var settings = this.service.channel(channelId).getOrDefault();

        assertThat(settings.getChannelId()).isEqualTo(channelId);
    }

    @Test
    void whenSettingAccessRole_accessRoleShouldBeSet() {
        long channelId = uniqueLong();
        long accessRoleId = uniqueLong();

        this.service.channel(channelId).setAccessRoleId(accessRoleId);

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getAccessRoleId()).hasValue(accessRoleId);
    }

    @Test
    void whenEnablingAutoOut_autoOutShouldBeEnabled() {
        long channelId = uniqueLong();

        this.service.channel(channelId).enableAutoOut();

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.isAutoOut()).isTrue();
    }

    @Test
    void whenDisablingAutoOut_autoOutShouldBeDisabled() {
        long channelId = uniqueLong();

        this.service.channel(channelId).disableAutoOut();

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.isAutoOut()).isFalse();
    }

    @Test
    void whenEnablingGameChannel_gameChannelShouldBeEnabled() {
        long channelId = uniqueLong();

        this.service.channel(channelId).enableGameChannel();

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.isGameChannel()).isTrue();
    }

    @Test
    void whenDisablingGameChannel_gameChannelShouldBeDisabled() {
        long channelId = uniqueLong();

        this.service.channel(channelId).disableGameChannel();

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.isGameChannel()).isFalse();
    }

    @Test
    void whenSettingTagCooldown_tagCooldownShouldBeSet() {
        long channelId = uniqueLong();
        long tagCooldown = uniqueLong();

        this.service.channel(channelId).setTagCooldown(tagCooldown);

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTagCooldownMinutes()).isEqualTo(tagCooldown);
    }

    @Test
    void whenTagUsed_tagUsedShouldBeSet() {
        long time = 1000;
        doReturn(time).when(this.clock).millis();

        long channelId = uniqueLong();

        this.service.channel(channelId).tagUsed();

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTagLastUsed()).isEqualTo(time);

        verify(clock).millis();
    }

    @Test
    void whenTagAdded_tagShouldBeAdded() {
        long channelId = uniqueLong();
        long tag = uniqueLong();

        this.service.channel(channelId).addTag(tag);

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).contains(tag);
    }

    @Test
    void whenExistingTagAdded_tagShouldNotBeDuplicated() {
        long channelId = uniqueLong();
        long tag = uniqueLong();
        this.repository.addTags(channelId, Set.of(tag));

        this.service.channel(channelId).addTag(tag);

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).containsOnlyOnce(tag);
    }

    @Test
    void whenTagsAdded_tagsShouldBeAdded() {
        long channelId = uniqueLong();
        long tagA = uniqueLong();
        long tagB = uniqueLong();

        this.service.channel(channelId).addTags(Set.of(tagA, tagB));

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).contains(tagA, tagB);
    }

    @Test
    void whenExistingTagsAdded_tagsShouldNotBeDuplicated() {
        long channelId = uniqueLong();
        long tagA = uniqueLong();
        long tagB = uniqueLong();
        this.repository.addTags(channelId, Set.of(tagA));

        this.service.channel(channelId).addTags(Set.of(tagA, tagB));

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).containsOnlyOnce(tagA, tagB);
    }

    @Test
    void whenTagRemoved_tagShouldNotBePresent() {
        long channelId = uniqueLong();
        long tag = uniqueLong();

        this.service.channel(channelId).removeTag(tag);

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).doesNotContain(tag);
    }

    @Test
    void whenExistingTagRemoved_tagShouldNotBePresent() {
        long channelId = uniqueLong();
        long tag = uniqueLong();
        this.repository.addTags(channelId, Set.of(tag));

        this.service.channel(channelId).removeTag(tag);

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).doesNotContain(tag);
    }

    @Test
    void whenTagsRemoved_tagsShouldNotBePresent() {
        long channelId = uniqueLong();
        long tagA = uniqueLong();
        long tagB = uniqueLong();

        this.service.channel(channelId).removeTags(Set.of(tagA, tagB));

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).doesNotContain(tagA, tagB);
    }

    @Test
    void whenExistingTagsRemoved_tagsShouldNotBePresent() {
        long channelId = uniqueLong();
        long tagA = uniqueLong();
        long tagB = uniqueLong();
        this.repository.addTags(channelId, Set.of(tagA));

        this.service.channel(channelId).removeTags(Set.of(tagA, tagB));

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();
        assertThat(settings.getTags()).doesNotContain(tagA, tagB);
    }

    @Test
    void whenNoTagsAdded_doNotInsertInDb() {
        long channelId = uniqueLong();

        this.service.channel(channelId).addTags(Set.of());

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNull();
    }

    @Test
    void whenNoTagsRemoved_doNotInsertInDb() {
        long channelId = uniqueLong();

        this.service.channel(channelId).removeTags(Set.of());

        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNull();
    }


    @Test
    void whenDelete_thenDeleteFromDb() {
        long channelId = uniqueLong();

        this.repository.setAccessRoleId(channelId, uniqueLong());
        var settings = this.repository.findOne(channelId);
        assertThat(settings).isNotNull();

        this.service.channel(channelId).reset();

        settings = this.repository.findOne(channelId);
        assertThat(settings).isNull();
    }
}
