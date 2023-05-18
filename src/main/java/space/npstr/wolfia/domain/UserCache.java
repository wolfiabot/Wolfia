/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.domain;

import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.common.Exceptions;

/**
 * Not a real cache but that's fine.
 */
@Component
public class UserCache {

    private static final String UNKNOWN_USER_NAME = "Unknown User";

    private final ShardManager shardManager;

    public UserCache(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    public Action user(long userId) {
        return new Action(userId, shardManager);
    }

    public static class Action {

        private final long userId;
        private final ShardManager shardManager;

        private Action(long userId, ShardManager shardManager) {
            this.userId = userId;
            this.shardManager = shardManager;
        }

        public Optional<User> fetch() {
            User user = shardManager.getUserById(this.userId);
            if (user != null) {
                return Optional.of(user);
            }

            return shardManager.retrieveUserById(this.userId).submit()
                    .handle((u, throwable) -> {
                        if (throwable != null) {
                            Throwable realCause = Exceptions.unwrap(throwable);
                            if (!(realCause instanceof ErrorResponseException)) {
                                throw new IllegalStateException("Unexpected exception when retrieving user", realCause);
                            }
                        }
                        return Optional.ofNullable(u);
                    })
                    .toCompletableFuture().join();
        }

        public String getName() {
            return fetch().map(User::getName).orElse(UNKNOWN_USER_NAME);
        }

        public String getEffectiveName(Optional<Guild> guild) {
            if (guild.isEmpty()) {
                return getName();
            }
            return getEffectiveName(guild.get().getIdLong());
        }

        public String getEffectiveName(long guildId) {
            Guild guild = shardManager.getGuildById(guildId);
            if (guild != null) {
                Member member = guild.getMemberById(this.userId);
                if (member != null) {
                    return member.getEffectiveName();
                }
            }

            return getName();
        }

        public Optional<String> getNick(long guildId) {
            Guild guild = shardManager.getGuildById(guildId);
            if (guild != null) {
                Member member = guild.getMemberById(this.userId);
                if (member != null) {
                    return Optional.ofNullable(member.getNickname());
                }
            }

            return Optional.empty();
        }

    }
}
