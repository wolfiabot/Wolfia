/*
 * Copyright (C) 2016-2020 the original author or authors
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.game.definitions.Roles;

import java.util.Set;
import java.util.function.Consumer;

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

        GameStats gameStats = new GameStats(guildId, guildName, channelId, channelName,
                gameType, mode, playerSize);
        gameStats.setEndTime(endTime);

        TeamStats wolves = new TeamStats(gameStats, Alignments.WOLF, "Wolves", 1);
        wolves.setWinner(false);
        TeamStats village = new TeamStats(gameStats, Alignments.VILLAGE, "Village", 2);
        village.setWinner(true);
        gameStats.setTeams(Set.of(wolves, village));

        PlayerStats wolf = new PlayerStats(wolves, uniqueLong(), "Wolfie", Alignments.WOLF, Roles.VANILLA);
        wolves.setPlayers(Set.of(wolf));

        PlayerStats villagerA = new PlayerStats(village, uniqueLong(), "Villagy McVillageface", Alignments.VILLAGE, Roles.VANILLA);
        PlayerStats villagerB = new PlayerStats(village, uniqueLong(), null, Alignments.VILLAGE, Roles.VANILLA);
        village.setPlayers(Set.of(villagerA, villagerB));

        //TODO test more actions
        ActionStats shot = new ActionStats(gameStats, 1, System.currentTimeMillis() - 5000, System.currentTimeMillis() - 4000,
                1, Phase.DAY, villagerA.getUserId(), Actions.SHOOT, wolf.getUserId(), null);

        gameStats.addAction(shot);

        assertThat(gameStats.getGameId()).isEmpty();
        assertThat(wolves.getTeamId()).isEmpty();
        assertThat(village.getTeamId()).isEmpty();
        assertThat(wolf.getPlayerId()).isEmpty();
        assertThat(villagerA.getPlayerId()).isEmpty();
        assertThat(villagerB.getPlayerId()).isEmpty();
        assertThat(shot.getActionId()).isEmpty();

        long gameId = this.repository.insertGameStats(gameStats)
                .toCompletableFuture().join().getGameId().orElseThrow();

        GameStats fetched = this.repository.findGameStats(gameId).toCompletableFuture().join().orElseThrow();

        assertThat(fetched.getGameId()).hasValue(gameId);
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
        assertThat(teams).filteredOnAssertions(isTeam(wolves, gameId)).hasSize(1);
        assertThat(teams).filteredOnAssertions(isTeam(village, gameId)).hasSize(1);

        TeamStats wolfTeam = teams.stream().filter(t -> t.getAlignment() == Alignments.WOLF).findAny().orElseThrow();
        assertThat(wolfTeam.getPlayers()).hasSize(1);
        assertThat(wolfTeam.getPlayers()).filteredOnAssertions(isPlayer(wolf, wolfTeam.getTeamId().orElseThrow()))
                .hasSize(1);

        TeamStats villageTeam = teams.stream().filter(t -> t.getAlignment() == Alignments.VILLAGE).findAny().orElseThrow();
        assertThat(villageTeam.getPlayers()).hasSize(2);
        assertThat(villageTeam.getPlayers()).filteredOnAssertions(isPlayer(villagerA, villageTeam.getTeamId().orElseThrow()))
                .hasSize(1);
        assertThat(villageTeam.getPlayers()).filteredOnAssertions(isPlayer(villagerB, villageTeam.getTeamId().orElseThrow()))
                .hasSize(1);

        Set<ActionStats> actions = fetched.getActions();
        assertThat(actions).hasSize(1);
        assertThat(actions).filteredOnAssertions(isAction(shot, gameId)).hasSize(1);
    }

    private Consumer<TeamStats> isTeam(TeamStats teamStats, long gameId) {
        return actual -> {
            assertThat(actual.getTeamId()).isPresent();
            assertThat(actual.getGame().getGameId()).hasValue(gameId);
            assertThat(actual.getAlignment()).isEqualTo(teamStats.getAlignment());
            assertThat(actual.getName()).isEqualTo(teamStats.getName());
            assertThat(actual.isWinner()).isEqualTo(teamStats.isWinner());
            assertThat(actual.getTeamSize()).isEqualTo(teamStats.getTeamSize());
        };
    }

    private Consumer<PlayerStats> isPlayer(PlayerStats playerStats, long teamId) {
        return actual -> {
            assertThat(actual.getPlayerId()).isPresent();
            assertThat(actual.getTeam().getTeamId()).hasValue(teamId);
            assertThat(actual.getUserId()).isEqualTo(playerStats.getUserId());
            assertThat(actual.getNickname()).isEqualTo(playerStats.getNickname());
            assertThat(actual.getAlignment()).isEqualTo(playerStats.getAlignment());
            assertThat(actual.getRole()).isEqualTo(playerStats.getRole());
            assertThat(actual.getTotalPosts()).isEqualTo(playerStats.getTotalPosts());
            assertThat(actual.getTotalPostLength()).isEqualTo(playerStats.getTotalPostLength());
        };
    }

    private Consumer<ActionStats> isAction(ActionStats actionStats, long gameId) {
        return actual -> {
            assertThat(actual.getActionId()).isPresent();
            assertThat(actual.getGame().getGameId()).hasValue(gameId);
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
