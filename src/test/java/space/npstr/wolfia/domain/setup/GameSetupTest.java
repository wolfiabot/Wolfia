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

import org.junit.jupiter.api.Test;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class GameSetupTest {

    private static final Games DEFAULT_GAME = Games.POPCORN;
    private static final GameInfo.GameMode DEFAULT_MODE = Games.getInfo(DEFAULT_GAME).getDefaultMode();
    private static final Duration DEFAULT_DAY_LENGTH = Duration.ofMinutes(5);

    @Test
    void defaults() {
        long channelId = uniqueLong();
        GameSetup setup = new GameSetup(channelId, new Long[]{}, null, null, null);

        assertThat(setup.getChannelId()).isEqualTo(channelId);
        assertThat(setup.getInnedUsers()).isEmpty();
        assertThat(setup.getGame()).isEqualTo(DEFAULT_GAME);
        assertThat(setup.getMode()).isEqualTo(DEFAULT_MODE);
        assertThat(setup.getDayLength()).isEqualTo(DEFAULT_DAY_LENGTH);
    }

}
