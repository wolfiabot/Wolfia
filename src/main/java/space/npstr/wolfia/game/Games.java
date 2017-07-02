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

import space.npstr.wolfia.utils.Emojis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by napster on 20.05.17.
 * <p>
 * keeps track of ongoing games
 */
//lists all games supported by the bot
public enum Games {
    POPCORN(Popcorn.class, Emojis.POPCORN + "-Mafia");

    //static aboose?
    private static final Map<Long, Game> GAME_REGISTRY = new HashMap<>();

    private static final Map<Class<? extends Game>, GameInfo> GAME_INFOS = new HashMap<>();

    static {
        GAME_INFOS.put(POPCORN.clazz, new PopcornInfo());
    }

    public static GameInfo getInfo(final Games game) {
        return GAME_INFOS.get(game.clazz);
    }

    public static GameInfo getInfo(final Class<? extends Game> gameClass) {
        return GAME_INFOS.get(gameClass);
    }

    public static GameInfo getInfo(final Game game) {
        return GAME_INFOS.get(game.getClass());
    }

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

    public static void remove(final long channelId) {
        GAME_REGISTRY.remove(channelId);
    }

    public static void set(final Game game) {
        GAME_REGISTRY.put(game.getChannelId(), game);
    }

    public final Class<? extends Game> clazz;
    public final String textRep;

    Games(final Class<? extends Game> clazz, final String textRepresentation) {
        this.clazz = clazz;
        this.textRep = textRepresentation;
    }

}
