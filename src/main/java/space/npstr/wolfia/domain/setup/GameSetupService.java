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

package space.npstr.wolfia.domain.setup;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Service
public class GameSetupService {

    private final GameSetupRepository repository;

    public GameSetupService(GameSetupRepository repository) {
        this.repository = repository;
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

        public GameSetup getOrDefault() {
            return repository.findOneOrDefault(this.channelId)
                    .toCompletableFuture().join();
        }

        public GameSetup setGame(Games game) {
            return repository.setGame(this.channelId, game)
                    .toCompletableFuture().join();
        }

        public GameSetup setMode(GameInfo.GameMode mode) {
            return repository.setMode(this.channelId, mode)
                    .toCompletableFuture().join();
        }

        public GameSetup setDayLength(Duration duration) {
            return repository.setDayLength(this.channelId, duration)
                    .toCompletableFuture().join();
        }

        public GameSetup inUser(long userId) {
            return inUsers(Set.of(userId));
        }

        public GameSetup inUsers(Set<Long> userIds) {
            if (userIds.isEmpty()) {
                return getOrDefault();
            }
            return repository.inUsers(this.channelId, userIds)
                    .toCompletableFuture().join();
        }

        public GameSetup outUser(long userId) {
            return outUsers(Set.of(userId));
        }

        public GameSetup outUsers(Set<Long> userIds) {
            if (userIds.isEmpty()) {
                return getOrDefault();
            }
            return repository.outUsers(this.channelId, userIds)
                    .toCompletableFuture().join();
        }

        public GameSetup clearInnedUsers() {
            return this.outUsers(getOrDefault().getInnedUsers());
        }

        public void reset() {
            repository.delete(this.channelId)
                    .toCompletableFuture().join();
        }

        /**
         * Like {@link Action#getOrDefault()}, but cleans up left/inactive players first.
         */
        public GameSetup cleanUpInnedPlayers(ShardManager shardManager) {
            Set<Long> toBeOuted = new HashSet<>();
            Guild guild = getChannel(shardManager).getGuild();
            GameSetup setup = getOrDefault();
            setup.getInnedUsers().forEach(userId -> {
                //did they leave the guild?
                if (guild.getMemberById(userId) == null) {
                    toBeOuted.add(userId);
                }
            });
            return outUsers(toBeOuted);

            //TODO whenever time based ins are a thing, this is probably the place to check them
        }

        private TextChannel getChannel(ShardManager shardManager) {
            TextChannel tc = shardManager.getTextChannelById(this.channelId);
            if (tc == null) {
                throw new NullPointerException(String.format("Could not find channel %s of setup", this.channelId));
            }
            return tc;
        }

    }
}
