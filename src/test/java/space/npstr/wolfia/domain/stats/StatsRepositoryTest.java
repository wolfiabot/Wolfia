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

package space.npstr.wolfia.domain.stats;

import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.game.definitions.Roles;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class StatsRepositoryTest extends ApplicationTest {

    @Autowired
    private StatsRepository repository;

    @Test
    void insertGameStats_fetchGameStats_shouldBeEqualAndHaveGeneratedIds() {

        long guildId = uniqueLong();
        String guildName = "Foo";
        long channelId = uniqueLong();
        String channelName = "Bar";
        Games gameType = Games.POPCORN;
        GameInfo.GameMode mode = GameInfo.GameMode.CLASSIC;
        int playerSize = 3;
        long endTime = System.currentTimeMillis();

        var gameStats = new InsertGameStats(guildId, guildName, channelId, channelName,
                gameType, mode, playerSize);
        gameStats.setEndTime(endTime);

        var wolves = new InsertTeamStats(Alignments.WOLF, "Wolves", 1);
        wolves.setWinner(false);
        var village = new InsertTeamStats(Alignments.VILLAGE, "Village", 2);
        village.setWinner(true);
        gameStats.setTeams(Set.of(wolves, village));

        var wolf = new InsertPlayerStats(uniqueLong(), "Wolfie", Alignments.WOLF, Roles.VANILLA);
        wolves.setPlayers(Set.of(wolf));

        var villagerA = new InsertPlayerStats(uniqueLong(), "Villagy McVillageface", Alignments.VILLAGE, Roles.VANILLA);
        var villagerB = new InsertPlayerStats(uniqueLong(), null, Alignments.VILLAGE, Roles.VANILLA);
        village.setPlayers(Set.of(villagerA, villagerB));

        //TODO test more actions
        var shot = new InsertActionStats(1, System.currentTimeMillis() - 5000, System.currentTimeMillis() - 4000,
                1, Phase.DAY, villagerA.getUserId(), Actions.SHOOT, wolf.getUserId(), null);

        gameStats.addAction(shot);

        long gameId = this.repository.insertGameStats(gameStats).getGameId();

        GameStats fetched = this.repository.findGameStats(gameId);

        assertThat(fetched).isNotNull();
        assertThat(fetched.getGameId()).isEqualTo(gameId);
        assertThat(fetched.getGuildId()).isEqualTo(guildId);
        assertThat(fetched.getGuildName()).isEqualTo(guildName);
        assertThat(fetched.getChannelId()).isEqualTo(channelId);
        assertThat(fetched.getChannelName()).isEqualTo(channelName);
        assertThat(fetched.getGameType()).isEqualTo(gameType);
        assertThat(fetched.getGameMode()).isEqualTo(mode);
        assertThat(fetched.getPlayerSize()).isEqualTo(playerSize);
        assertThat(fetched.getEndTime()).isEqualTo(endTime);
        // TODO start time?

        Set<TeamStats> teams = fetched.getStartingTeams();
        assertThat(teams).hasSize(2);
        assertThat(teams).filteredOnAssertions(equalsTeam(wolves)).hasSize(1);
        assertThat(teams).filteredOnAssertions(equalsTeam(village)).hasSize(1);

        TeamStats wolfTeam = teams.stream().filter(t -> t.getAlignment() == Alignments.WOLF).findAny().orElseThrow();
        assertThat(wolfTeam.getPlayers()).hasSize(1);
        assertThat(wolfTeam.getPlayers()).filteredOnAssertions(equalsPlayer(wolf))
                .hasSize(1);

        TeamStats villageTeam = teams.stream().filter(t -> t.getAlignment() == Alignments.VILLAGE).findAny().orElseThrow();
        assertThat(villageTeam.getPlayers()).hasSize(2);
        assertThat(villageTeam.getPlayers()).filteredOnAssertions(equalsPlayer(villagerA))
                .hasSize(1);
        assertThat(villageTeam.getPlayers()).filteredOnAssertions(equalsPlayer(villagerB))
                .hasSize(1);

        Set<ActionStats> actions = fetched.getActions();
        assertThat(actions).hasSize(1);
        assertThat(actions).filteredOnAssertions(equalsAction(shot)).hasSize(1);
    }

    private Consumer<TeamStats> equalsTeam(InsertTeamStats teamStats) {
        return actual -> {
            assertThat(actual.getAlignment()).isEqualTo(teamStats.getAlignment());
            assertThat(actual.getName()).isEqualTo(teamStats.getName());
            assertThat(actual.isWinner()).isEqualTo(teamStats.isWinner());
            assertThat(actual.getTeamSize()).isEqualTo(teamStats.getTeamSize());
        };
    }

    private Consumer<PlayerStats> equalsPlayer(InsertPlayerStats playerStats) {
        return actual -> {
            assertThat(actual.getUserId()).isEqualTo(playerStats.getUserId());
            assertThat(actual.getNickname()).isEqualTo(playerStats.getNickname());
            assertThat(actual.getAlignment()).isEqualTo(playerStats.getAlignment());
            assertThat(actual.getRole()).isEqualTo(playerStats.getRole());
            assertThat(actual.getTotalPosts()).isEqualTo(playerStats.getTotalPosts());
            assertThat(actual.getTotalPostLength()).isEqualTo(playerStats.getTotalPostLength());
        };
    }

    private Consumer<ActionStats> equalsAction(InsertActionStats actionStats) {
        return actual -> {
            assertThat(actual.getOrder()).isEqualTo(actionStats.getOrder());
            assertThat(actual.getTimeStampSubmitted()).isEqualTo(actionStats.getTimeStampSubmitted());
            assertThat(actual.getTimeStampHappened()).isEqualTo(actionStats.getTimeStampHappened());
            assertThat(actual.getCycle()).isEqualTo(actionStats.getCycle());
            assertThat(actual.getPhase()).isEqualTo(actionStats.getPhase());
            assertThat(actual.getActor()).isEqualTo(actionStats.getActor());
            assertThat(actual.getActionType()).isEqualTo(actionStats.getActionType());
            assertThat(actual.getTarget()).isEqualTo(actionStats.getTarget());
            assertThat(actual.getAdditionalInfo()).isEqualTo(actionStats.getAdditionalInfo());
        };
    }

}
