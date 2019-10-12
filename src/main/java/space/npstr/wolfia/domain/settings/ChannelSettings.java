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

import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Optional;
import java.util.Set;

public class ChannelSettings {

    private static final long DEFAULT_TAG_COOLDOWN_MINUTES = 5;

    private final long channelId;
    private final Optional<Long> accessRoleId;
    private final Optional<Long> tagCooldownMinutes;
    private final long tagLastUsed;
    private final Set<Long> tags;

    @ConstructorProperties({"channelId", "accessRoleId", "tagCooldown", "tagLastUsed", "tags"})
    public ChannelSettings(long channelId, @Nullable Long accessRoleId, @Nullable Long tagCooldownMinutes, long tagLastUsed, Long[] tags) {
        this.channelId = channelId;
        this.accessRoleId = Optional.ofNullable(accessRoleId);
        this.tagCooldownMinutes = Optional.ofNullable(tagCooldownMinutes);
        this.tagLastUsed = tagLastUsed;
        this.tags = Set.of(tags);
    }

    public long getChannelId() {
        return this.channelId;
    }

    public Optional<Long> getAccessRoleId() {
        return this.accessRoleId;
    }

    public long getTagCooldownMinutes() {
        return this.tagCooldownMinutes.orElse(DEFAULT_TAG_COOLDOWN_MINUTES);
    }

    public long getTagLastUsed() {
        return this.tagLastUsed;
    }

    public Set<Long> getTags() {
        return this.tags;
    }
}
