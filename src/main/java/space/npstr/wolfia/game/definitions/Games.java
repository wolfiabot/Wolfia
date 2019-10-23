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

package space.npstr.wolfia.game.definitions;

import net.dv8tion.jda.api.entities.TextChannel;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.mafia.Mafia;
import space.npstr.wolfia.game.mafia.MafiaInfo;
import space.npstr.wolfia.game.popcorn.Popcorn;
import space.npstr.wolfia.game.popcorn.PopcornInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by napster on 20.05.17.
 * <p>
 * keeps track of ongoing games
 */
//lists all games supported by the bot
public enum Games {
    POPCORN(Popcorn.class, "Popcorn"),
    MAFIA(Mafia.class, "Mafia");

    public final Class<? extends Game> clazz;
    public final String textRep;

    Games(final Class<? extends Game> clazz, final String textRepresentation) {
        this.clazz = clazz;
        this.textRep = textRepresentation;
    }

    private static final Map<Long, Game> GAME_REGISTRY = new ConcurrentHashMap<>();

    private static final Map<Class<? extends Game>, GameInfo> GAME_INFOS = new HashMap<>();

    static {
        GAME_INFOS.put(POPCORN.clazz, new PopcornInfo());
        GAME_INFOS.put(MAFIA.clazz, new MafiaInfo());
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
        return new HashMap<>(GAME_REGISTRY);
    }

    /**
     * @return game that is running in the specified channel; may return null
     */
    @Nullable
    public static Game get(final long channelId) {
        return GAME_REGISTRY.get(channelId);
    }

    @Nullable
    public static Game get(@Nonnull final TextChannel channel) {
        return get(channel.getIdLong());
    }

    //useful for evaling
    public static Game get(final String channelId) {
        return GAME_REGISTRY.get(Long.valueOf(channelId));
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

    public static int getRunningGamesCount() {
        return GAME_REGISTRY.size();
    }
}
