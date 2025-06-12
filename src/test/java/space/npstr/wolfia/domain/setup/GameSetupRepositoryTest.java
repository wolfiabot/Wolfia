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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.domain.settings.ChannelSettingsService;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class GameSetupRepositoryTest extends ApplicationTest {

    private static final Games DEFAULT_GAME = Games.POPCORN;
    private static final GameInfo.GameMode DEFAULT_MODE = Games.getInfo(DEFAULT_GAME).getDefaultMode();
    private static final Duration DEFAULT_DAY_LENGTH = Duration.ofMinutes(5);

    @Autowired
    private GameSetupRepository repository;

    @Autowired
    private ChannelSettingsService channelSettingsService;

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_expectDefaultValues() {
        long channelId = uniqueLong();

        var setup = this.repository.findOneOrDefault(channelId);

        assertThat(setup.getChannelId()).isEqualTo(channelId);
        assertThat(setup.getInnedUsers()).isEmpty();
        assertThat(setup.getGame()).isEqualTo(DEFAULT_GAME);
        assertThat(setup.getMode()).isEqualTo(DEFAULT_MODE);
        assertThat(setup.getDayLength()).isEqualTo(DEFAULT_DAY_LENGTH);
    }

    @Test
    void givenEntryDoesNotExist_whenFetchingDefault_doNotCreateEntry() {
        long channelId = uniqueLong();

        var setup = this.repository.findOneOrDefault(channelId);

        assertThat(setup.getChannelId()).isEqualTo(channelId);
        var created = this.repository.findOne(channelId);
        assertThat(created).isNull();
    }

    @Test
    void givenExistingEntry_whenFetchingDefault_returnExistingEntry() {
        long channelId = uniqueLong();
        Games game = Games.MAFIA;
        // ensure this test stays viable when defaults are changed
        assertThat(game).isNotEqualTo(DEFAULT_GAME);
        this.repository.setGame(channelId, game);

        var settings = this.repository.findOneOrDefault(channelId);

        assertThat(settings.getChannelId()).isEqualTo(channelId);
        assertThat(settings.getInnedUsers()).isEmpty();
        assertThat(settings.getGame()).isEqualTo(game);
        assertThat(settings.getMode()).isEqualTo(Games.getInfo(game).getDefaultMode());
    }

    @Test
    void givenUserInnedInOneAutoOutSetup_whenFindAutoOutSetupsWhereUserIsInned_returnOneAutoOutSetup() {
        long channelId = uniqueLong();
        long userId = uniqueLong();
        this.channelSettingsService.channel(channelId).enableAutoOut();
        this.repository.inUsers(channelId, Set.of(userId));

        List<GameSetup> setups = this.repository.findAutoOutSetupsWhereUserIsInned(userId);

        assertThat(setups)
                .singleElement()
                .satisfies(isSetupInChannel(channelId));
    }

    @Test
    void givenUserInnedInMultipleAutoOutSetups_whenFindAutoOutSetupsWhereUserIsInned_returnAllAutoOutSetupWhereUserInned() {
        long channelIdA = uniqueLong();
        long channelIdB = uniqueLong();
        long userId = uniqueLong();
        this.channelSettingsService.channel(channelIdA).enableAutoOut();
        this.channelSettingsService.channel(channelIdB).enableAutoOut();
        this.repository.inUsers(channelIdA, Set.of(userId));
        this.repository.inUsers(channelIdB, Set.of(userId));

        List<GameSetup> setups = this.repository.findAutoOutSetupsWhereUserIsInned(userId);

        assertThat(setups).hasSize(2);
        assertThat(setups).filteredOnAssertions(isSetupInChannel(channelIdA)).hasSize(1);
        assertThat(setups).filteredOnAssertions(isSetupInChannel(channelIdB)).hasSize(1);
    }

    @Test
    void givenUserInnedInMultipleSetups_whenFindAutoOutSetupsWhereUserIsInned_returnAllAutoOutSetupWhereUserInned() {
        long channelIdAutoOut = uniqueLong();
        long channelIdNoAutoOut = uniqueLong();
        long userId = uniqueLong();
        this.channelSettingsService.channel(channelIdAutoOut).enableAutoOut();
        this.channelSettingsService.channel(channelIdNoAutoOut).disableAutoOut();
        this.repository.inUsers(channelIdAutoOut, Set.of(userId));
        this.repository.inUsers(channelIdNoAutoOut, Set.of(userId));

        List<GameSetup> setups = this.repository.findAutoOutSetupsWhereUserIsInned(userId);

        assertThat(setups).hasSize(1);
        assertThat(setups).filteredOnAssertions(isSetupInChannel(channelIdAutoOut)).hasSize(1);
    }

    private Consumer<GameSetup> isSetupInChannel(long channelId) {
        return actual -> assertThat(actual.getChannelId()).isEqualTo(channelId);
    }

}
