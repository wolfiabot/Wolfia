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

package space.npstr.wolfia.domain.setup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class GameSetupServiceTest extends ApplicationTest {

    @Autowired
    private GameSetupService service;

    @Autowired
    private GameSetupRepository repository;


    @Test
    void whenGetting_correctSetupsIsReturned() {
        long channelId = uniqueLong();

        var setup = this.service.channel(channelId).getOrDefault();

        assertThat(setup.getChannelId()).isEqualTo(channelId);
    }


    @Test
    void whenGameSet_gameShouldBeSet() {
        long channelId = uniqueLong();
        Games game = Games.MAFIA;

        this.service.channel(channelId).setGame(game);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getGame()).isEqualTo(game);
    }

    @Test
    void whenIncompatibleGameSet_incompatibleGamesDefaultModeShouldBeReturned() {
        long channelId = uniqueLong();
        Games game = Games.POPCORN;
        GameInfo.GameMode mode = GameInfo.GameMode.WILD;
        this.repository.setGame(channelId, game).toCompletableFuture().join();
        this.repository.setMode(channelId, mode).toCompletableFuture().join();
        Games incompatibleGame = Games.MAFIA;
        GameInfo incompatibleGameInfo = Games.getInfo(incompatibleGame);
        //make sure this test stays relevant despite possible future changes to which modes are supported by mafia
        assertThat(incompatibleGameInfo.getSupportedModes()).doesNotContain(mode);

        this.service.channel(channelId).setGame(incompatibleGame);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getMode()).isEqualTo(incompatibleGameInfo.getDefaultMode());
    }

    @Test
    void whenModeSet_modeShouldBeSet() {
        long channelId = uniqueLong();
        GameInfo.GameMode mode = GameInfo.GameMode.CLASSIC;
        //ensure that the mdoe is compatible with the game
        this.repository.setGame(channelId, Games.POPCORN).toCompletableFuture().join();

        this.service.channel(channelId).setMode(mode);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getMode()).isEqualTo(mode);
    }

    @Test
    void whenIncompatibleModeSet_gamesDefaultModeShouldBeReturned() {
        long channelId = uniqueLong();
        Games game = Games.POPCORN;
        GameInfo.GameMode incompatibleMode = GameInfo.GameMode.XMAS;
        GameInfo.GameMode defaultMode = Games.getInfo(game).getDefaultMode();
        //make sure this test stays relevant despite possible future changes to defaults
        assertThat(incompatibleMode).isNotEqualTo(defaultMode);
        this.repository.setGame(channelId, game).toCompletableFuture().join();

        this.service.channel(channelId).setMode(incompatibleMode); //there is no xmas popcorn

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getMode()).isEqualTo(defaultMode);
    }

    @Test
    void whenDayLengthIsSet_dayLengthShouldBeSet() {
        long channelId = uniqueLong();
        Duration dayLenth = Duration.ofSeconds(42);

        this.service.channel(channelId).setDayLength(dayLenth);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getDayLength()).isEqualTo(dayLenth);
    }


    @Test
    void givenUserIsOut_whenUserInned_userShouldBeInned() {
        long channelId = uniqueLong();
        long userId = uniqueLong();

        this.service.channel(channelId).inUser(userId);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).contains(userId);
    }

    @Test
    void givenUserIsIn_whenUserInned_userShouldNotBeDuplicated() {
        long channelId = uniqueLong();
        long userId = uniqueLong();
        this.repository.inUsers(channelId, Set.of(userId)).toCompletableFuture().join();

        this.service.channel(channelId).inUser(userId);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).containsOnlyOnce(userId);
    }

    @Test
    void givenUsersAreOut_whenUsersInned_usersShouldBeInned() {
        long channelId = uniqueLong();
        long userA = uniqueLong();
        long userB = uniqueLong();

        this.service.channel(channelId).inUsers(Set.of(userA, userB));

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).contains(userA, userB);
    }

    @Test
    void givenUsersAreIn_whenUsersInned_usersShouldNotBeDuplicated() {
        long channelId = uniqueLong();
        long userA = uniqueLong();
        long userB = uniqueLong();
        this.repository.inUsers(channelId, Set.of(userA)).toCompletableFuture().join();

        this.service.channel(channelId).inUsers(Set.of(userA, userB));

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).containsOnlyOnce(userA, userB);
    }

    @Test
    void givenUserOut_whenUserOuted_userShouldNotBeIn() {
        long channelId = uniqueLong();
        long userId = uniqueLong();

        this.service.channel(channelId).outUser(userId);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).doesNotContain(userId);
    }

    @Test
    void givenUserIn_whenUserOuted_userShouldNotBeIn() {
        long channelId = uniqueLong();
        long userId = uniqueLong();
        this.repository.inUsers(channelId, Set.of(userId)).toCompletableFuture().join();

        this.service.channel(channelId).outUser(userId);

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).doesNotContain(userId);
    }

    @Test
    void givenUsersAreOut_whenUsersOuted_usersShouldNotBeIn() {
        long channelId = uniqueLong();
        long userA = uniqueLong();
        long userB = uniqueLong();

        this.service.channel(channelId).outUsers(Set.of(userA, userB));

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).doesNotContain(userA, userB);
    }

    @Test
    void givenUsersAreIn_whenUsersOuted_userssShouldNotBeIn() {
        long channelId = uniqueLong();
        long userA = uniqueLong();
        long userB = uniqueLong();
        this.repository.inUsers(channelId, Set.of(userA)).toCompletableFuture().join();

        this.service.channel(channelId).outUsers(Set.of(userA, userB));

        var setup = this.repository.findOne(channelId).toCompletableFuture().join().orElseThrow();
        assertThat(setup.getInnedUsers()).doesNotContain(userA, userB);
    }


    @Test
    void whenDelete_thenDeleteFromDb() {
        long channelId = uniqueLong();

        this.repository.setDayLength(channelId, Duration.ofSeconds(42)).toCompletableFuture().join();
        var setup = this.repository.findOne(channelId).toCompletableFuture().join();
        assertThat(setup).isPresent();

        this.service.channel(channelId).reset();

        setup = this.repository.findOne(channelId).toCompletableFuture().join();
        assertThat(setup).isEmpty();
    }

}
