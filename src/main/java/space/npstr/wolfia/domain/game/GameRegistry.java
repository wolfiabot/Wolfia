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

package space.npstr.wolfia.domain.game;

import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.game.Game;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keep track of ongoing games
 */
@Component
public class GameRegistry {

    private final Map<Long, Game> games = new ConcurrentHashMap<>();

    public Map<Long, Game> getAll() {
        return Collections.unmodifiableMap(this.games);
    }

    /**
     * @return game that is running in the specified channel; may return null
     */
    @Nullable
    public Game get(final long channelId) {
        return this.games.get(channelId);
    }

    @Nullable
    public Game get(@Nonnull final TextChannel channel) {
        return get(channel.getIdLong());
    }

    //useful for evaling
    public Game get(final String channelId) {
        return this.games.get(Long.valueOf(channelId));
    }

    public void remove(final Game game) {
        remove(game.getChannelId());
    }

    public void remove(final long channelId) {
        this.games.remove(channelId);
    }

    public void set(final Game game) {
        this.games.put(game.getChannelId(), game);
    }

    public int getRunningGamesCount() {
        return this.games.size();
    }

}
