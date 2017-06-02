/*
 * Copyright (C) 2017 Dennis Neufeld
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

package space.npstr.wolfia.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by napster on 20.05.17.
 * <p>
 * keeps track of ongoing games
 */
public class Games {

    //lists all games supported by the bot
    public enum GAME {
        POPCORN(Popcorn.class);

        private final Class<? extends Game> clazz;

        GAME(final Class<? extends Game> clazz) {
            this.clazz = clazz;
        }

        public Class<? extends Game> getGameClass() {
            return this.clazz;
        }

        public Game createInstance() {
            try {
                return this.clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                return new Popcorn(); //todo decide if this handling is ok
            }
        }
    }


    //static aboose
    private static final Map<Long, Game> GAME_REGISTRY = new HashMap<>();

    public static Map<Long, Game> getAll() {
        return Collections.unmodifiableMap(GAME_REGISTRY);
    }

    /**
     * @return game that is running in the specified channel; may return null
     */
    public static Game get(final long channelId) {
        return GAME_REGISTRY.get(channelId);
    }

    public static void remove(final Game game) {
        GAME_REGISTRY.remove(game.getChannelId());
    }

    public static void set(final Game game) {
        GAME_REGISTRY.put(game.getChannelId(), game);
    }
}
