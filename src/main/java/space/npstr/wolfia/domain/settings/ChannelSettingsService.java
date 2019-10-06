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

import org.springframework.stereotype.Service;
import space.npstr.wolfia.db.gen.tables.records.ChannelSettingsRecord;

import java.time.Clock;
import java.util.Collection;
import java.util.Set;

@Service
public class ChannelSettingsService {

    private final ChannelSettingsRepository repository;
    private final Clock clock;

    public ChannelSettingsService(ChannelSettingsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * This service has many calls that require passing in multiple long ids. This fluent action api should help avoid
     * mistakes where arguments are passed in the wrong order.
     *
     * @return an action that can be executed on the passed in channel
     */
    public Action channel(long channelId) {
        return new Action(channelId);
    }

    public class Action {

        private final long channelId;

        private Action(long channelId) {
            this.channelId = channelId;
        }

        public ChannelSettingsRecord getOrDefault() {
            return repository.findOneOrDefault(this.channelId)
                    .toCompletableFuture().join();
        }

        public ChannelSettingsRecord setAccessRoleId(long accessRoleId) {
            return repository.setAccessRoleId(this.channelId, accessRoleId)
                    .toCompletableFuture().join();
        }

        public ChannelSettingsRecord setTagCooldown(long tagCooldown) {
            return repository.setTagCooldown(this.channelId, tagCooldown)
                    .toCompletableFuture().join();
        }

        public ChannelSettingsRecord tagUsed() {
            return repository.setTagLastUsed(this.channelId, clock.millis())
                    .toCompletableFuture().join();
        }

        public ChannelSettingsRecord addTag(long tag) {
            return addTags(Set.of(tag));
        }

        public ChannelSettingsRecord addTags(Collection<Long> tags) {
            return repository.addTags(this.channelId, tags)
                    .toCompletableFuture().join();
        }

        public ChannelSettingsRecord removeTag(long tag) {
            return removeTags(Set.of(tag));
        }

        public ChannelSettingsRecord removeTags(Collection<Long> tags) {
            return repository.removeTags(this.channelId, tags)
                    .toCompletableFuture().join();
        }
    }

}
