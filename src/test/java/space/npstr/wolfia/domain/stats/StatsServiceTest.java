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

package space.npstr.wolfia.domain.stats;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Roles;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class StatsServiceTest extends ApplicationTest {

    @Autowired
    private StatsRepository statsRepository;

    @Test
    void whenAnonymize_playerNameIsAnonymized() {
        var gameStats = new InsertGameStats(uniqueLong(), "Foo", uniqueLong(), "Bar", Games.POPCORN, GameInfo.GameMode.WILD, 1);
        var village = new InsertTeamStats(Alignments.VILLAGE, "Village", 1);
        village.setWinner(true);
        var playerStats = new InsertPlayerStats(uniqueLong(), "Player McPlayerface", Alignments.VILLAGE, Roles.COP);
        village.addPlayer(playerStats);
        gameStats.setTeams(List.of(village));
        var recorded = statsService.recordGameStats(gameStats);

        PlayerStats player = recorded.getStartingTeams().stream().findAny().orElseThrow()
                .getPlayers().stream().findAny().orElseThrow();
        assertThat(player.getNickname()).isEqualTo("Player McPlayerface");

        statsService.anonymize(player.getUserId());

        player = requireNonNull(statsRepository.findGameStats(recorded.getGameId()))
                .getStartingTeams().stream().findAny().orElseThrow()
                .getPlayers().stream().findAny().orElseThrow();
        assertThat(player.getNickname()).isNull();
    }

}
