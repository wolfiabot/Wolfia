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

package space.npstr.wolfia.domain.game;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.game.Game;

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
    public Game get(long channelId) {
        return this.games.get(channelId);
    }

    @Nullable
    public Game get(TextChannel channel) {
        return get(channel.getIdLong());
    }

    //useful for evaling
    public Game get(String channelId) {
        return this.games.get(Long.valueOf(channelId));
    }

    public void remove(Game game) {
        remove(game.getChannelId());
    }

    public void remove(long channelId) {
        this.games.remove(channelId);
    }

    public void set(Game game) {
        this.games.put(game.getChannelId(), game);
    }

    public int getRunningGamesCount() {
        return this.games.size();
    }

}
