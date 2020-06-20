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

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ChannelSettingsService {

    private final ChannelSettingsRepository repository;
    private final Clock clock;

    public ChannelSettingsService(ChannelSettingsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public MultiAction channels(Collection<Long> channelIds) {
        return new MultiAction(channelIds);
    }

    public class MultiAction {
        private final Collection<Long> channelIds;

        private MultiAction(Collection<Long> channelIds) {
            this.channelIds = channelIds;
        }

        public List<ChannelSettings> getOrDefault() {
            return repository.findOrDefault(channelIds)
                    .toCompletableFuture().join();
        }
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

        public ChannelSettings getOrDefault() {
            return repository.findOneOrDefault(this.channelId)
                    .toCompletableFuture().join();
        }

        public ChannelSettings setAccessRoleId(long accessRoleId) {
            return repository.setAccessRoleId(this.channelId, accessRoleId)
                    .toCompletableFuture().join();
        }

        public ChannelSettings enableAutoOut() {
            return repository.setAutoOut(this.channelId, true)
                    .toCompletableFuture().join();
        }

        public ChannelSettings disableAutoOut() {
            return repository.setAutoOut(this.channelId, false)
                    .toCompletableFuture().join();
        }

        public ChannelSettings enableGameChannel() {
            return repository.setGameChannel(this.channelId, true)
                    .toCompletableFuture().join();
        }

        public ChannelSettings disableGameChannel() {
            return repository.setGameChannel(this.channelId, false)
                    .toCompletableFuture().join();
        }

        public ChannelSettings setTagCooldown(long tagCooldown) {
            return repository.setTagCooldown(this.channelId, tagCooldown)
                    .toCompletableFuture().join();
        }

        public ChannelSettings tagUsed() {
            return repository.setTagLastUsed(this.channelId, clock.millis())
                    .toCompletableFuture().join();
        }

        public ChannelSettings addTag(long tag) {
            return addTags(Set.of(tag));
        }

        public ChannelSettings addTags(Collection<Long> tags) {
            if (tags.isEmpty()) {
                return getOrDefault();
            }
            return repository.addTags(this.channelId, tags)
                    .toCompletableFuture().join();
        }

        public ChannelSettings removeTag(long tag) {
            return removeTags(Set.of(tag));
        }

        public ChannelSettings removeTags(Collection<Long> tags) {
            if (tags.isEmpty()) {
                return getOrDefault();
            }
            return repository.removeTags(this.channelId, tags)
                    .toCompletableFuture().join();
        }

        public void reset() {
            repository.delete(this.channelId)
                    .toCompletableFuture().join();
        }
    }

}
