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

package space.npstr.wolfia.domain;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.common.Exceptions;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Not a real cache but that's fine.
 */
@Component
public class UserCache {

    private static final String UNKNOWN_USER_NAME = "Unknown User";

    @CheckReturnValue
    public Action user(long userId) {
        return new Action(userId);
    }

    public static class Action {

        private final long userId;

        private Action(long userId) {
            this.userId = userId;
        }

        public Optional<User> get() {
            User user = getShardManager().getUserById(this.userId);
            if (user != null) {
                return Optional.of(user);
            }

            return getShardManager().retrieveUserById(this.userId).submit()
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

        @CheckReturnValue
        public String getName() {
            return get().map(User::getName).orElse(UNKNOWN_USER_NAME);
        }

        @CheckReturnValue
        public String getEffectiveName(@Nullable Guild guild) {
            if (guild == null) {
                return getName();
            }
            return getEffectiveName(guild.getIdLong());
        }

        @CheckReturnValue
        public String getEffectiveName(long guildId) {
            Guild guild = getShardManager().getGuildById(guildId);
            if (guild != null) {
                Member member = guild.getMemberById(this.userId);
                if (member != null) {
                    return member.getEffectiveName();
                }
            }

            return getName();
        }

        @CheckReturnValue
        public Optional<String> getNick(long guildId) {
            Guild guild = getShardManager().getGuildById(guildId);
            if (guild != null) {
                Member member = guild.getMemberById(this.userId);
                if (member != null) {
                    return Optional.ofNullable(member.getNickname());
                }
            }

            return Optional.empty();
        }

        // avoid circular bean dependency for now
        private ShardManager getShardManager() {
            return Launcher.getBotContext().getShardManager();
        }
    }
}
