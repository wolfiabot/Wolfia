/*
 * Copyright (C) 2016-2025 the original author or authors
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

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

public class GameSetup {

    private static final Games DEFAULT_GAME = Games.POPCORN;
    private static final Duration DEFAULT_DAY_LENGTH = Duration.ofMinutes(5);

    private final long channelId;
    private final Set<Long> innedUsers;
    private final Optional<Games> game;
    private final Optional<GameInfo.GameMode> mode;
    private final Optional<Duration> dayLength;

    public GameSetup(long channelId, Long[] innedUsers, @Nullable String game, @Nullable String mode,
                     @Nullable Long dayLength) {

        this.channelId = channelId;
        this.innedUsers = Set.of(innedUsers);
        this.game = Optional.ofNullable(game).map(Games::valueOf);
        this.mode = Optional.ofNullable(mode).map(GameInfo.GameMode::valueOf);
        this.dayLength = Optional.ofNullable(dayLength).map(Duration::ofMillis);
    }

    public long getChannelId() {
        return this.channelId;
    }

    public Set<Long> getInnedUsers() {
        return this.innedUsers;
    }

    public boolean isIn(long userId) {
        return this.getInnedUsers().contains(userId);
    }

    public Games getGame() {
        return this.game.orElse(DEFAULT_GAME);
    }

    public GameInfo.GameMode getMode() {
        GameInfo gameInfo = Games.getInfo(getGame());
        GameInfo.GameMode gameMode = this.mode.orElse(gameInfo.getDefaultMode());

        // ensure that the mode is compatible with the game that has been set
        if (!gameInfo.getSupportedModes().contains(gameMode)) {
            return gameInfo.getDefaultMode();
        }

        return gameMode;
    }

    public Duration getDayLength() {
        return this.dayLength.orElse(DEFAULT_DAY_LENGTH);
    }
}
