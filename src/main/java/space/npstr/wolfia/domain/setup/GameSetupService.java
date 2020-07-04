/*
 * Copyright (C) 2016-2020 the original author or authors
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
import java.util.List;
import java.util.Set;

import static space.npstr.wolfia.utils.discord.TextchatUtils.userAsMention;

@Service
public class GameSetupService {

    private final GameSetupRepository repository;

    public GameSetupService(GameSetupRepository repository) {
        this.repository = repository;
    }

    public void outUserDueToInactivity(long userId, ShardManager shardManager) {
        List<GameSetup> setups = this.repository.findAutoOutSetupsWhereUserIsInned(userId)
                .toCompletableFuture().join();

        for (GameSetup setup : setups) {
            channel(setup.getChannelId())
                    .outUserDueToInactivity(userId, shardManager);
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

        public GameSetup outUserDueToInactivity(long userId, ShardManager shardManager) {
            GameSetup setup = getOrDefault();
            if (!setup.getInnedUsers().contains(userId)) {
                return setup;
            }
            TextChannel channel = shardManager.getTextChannelById(setup.getChannelId());
            if (channel != null) {
                channel.sendMessage(userAsMention(userId)
                        + " became inactive and were outed from the game setup.").queue();
            }

            return outUser(userId);
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
         * Like {@link Action#getOrDefault()}, but cleans up left/inactive players first if possible.
         */
        public GameSetup cleanUpInnedPlayers(ShardManager shardManager) {
            GameSetup setup = getOrDefault();

            TextChannel channel = shardManager.getTextChannelById(this.channelId);
            if (channel == null) {
                return setup;
            }

            Set<Long> toBeOuted = new HashSet<>();
            Guild guild = channel.getGuild();
            setup.getInnedUsers().forEach(userId -> {
                if (guild.getMemberById(userId) == null) {
                    toBeOuted.add(userId);
                    channel.sendMessage(userAsMention(userId)
                            + " has left this guild and was outed from the game setup.").queue();
                }
            });
            return outUsers(toBeOuted);
        }
    }
}
