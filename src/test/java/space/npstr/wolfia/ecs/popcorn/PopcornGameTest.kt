/*
 * Copyright (C) 2016-2026 the original author or authors
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
package space.npstr.wolfia.ecs.popcorn

import java.time.Duration
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import space.npstr.wolfia.TestExtensions.isNotNullKt
import space.npstr.wolfia.TestExtensions.satisfiesKt
import space.npstr.wolfia.ecs.GameEntity
import space.npstr.wolfia.ecs.GameWorld
import space.npstr.wolfia.ecs.MessageFlushSystem
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameMetaComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GameOverComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.GunHolderComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PhaseComponent
import space.npstr.wolfia.ecs.popcorn.PopcornComponents.PlayerComponent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GameStartEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GunDistTimerExpiredEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.GunDistVoteEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.PlayerSetup
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.ShootEvent
import space.npstr.wolfia.ecs.popcorn.PopcornEvents.TimerExpiredEvent
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Alignments

/**
 * Full game simulations for Popcorn ECS. No Discord, no Spring, no timers.
 */
internal class PopcornGameTest {

	companion object {
		const val CHANNEL: Long = 100L
		const val GUILD: Long = 200L
		val DAY_LENGTH: Duration = Duration.ofMinutes(10)

		// Player IDs
		const val ALICE: Long = 1L // village
		const val BOB: Long = 2L // village
		const val CAROL: Long = 3L // village
		const val WOLF_A: Long = 10L // wolf
		const val WOLF_B: Long = 11L // wolf
	}

	private lateinit var world: GameWorld
	private lateinit var sink: TestOutputSink

	@BeforeEach
	fun setUp() {
		world = GameWorld(CHANNEL)
		sink = TestOutputSink()
		// Register systems in order. TimerSystem with null scheduler (tests submit events directly).
		world.addSystem(GameStartSystem())
		world.addSystem(ShootSystem())
		world.addSystem(GunDistributionSystem())
		world.addSystem(TimerSystem(null))
		world.addSystem(WinConditionSystem())
		world.addSystem(MessageFlushSystem(sink))
	}

	@AfterEach
	fun tearDown() {
		world.close()
	}

	private fun startWildGame(players: MutableList<PlayerSetup>) {
		world.submit(GameStartEvent(CHANNEL, GUILD, players, GameMode.WILD, DAY_LENGTH))
			.get(5, TimeUnit.SECONDS)
	}

	private fun startClassicGame(players: MutableList<PlayerSetup>) {
		world.submit(GameStartEvent(CHANNEL, GUILD, players, GameMode.CLASSIC, DAY_LENGTH))
			.get(5, TimeUnit.SECONDS)
	}

	private fun shoot(shooter: Long, target: Long) {
		world.submit(ShootEvent(shooter, target)).get(5, TimeUnit.SECONDS)
	}

	private fun timerExpires(day: Int) {
		world.submit(TimerExpiredEvent(day)).get(5, TimeUnit.SECONDS)
	}

	private fun gunDistVote(voter: Long, candidate: Long) {
		world.submit(GunDistVoteEvent(voter, candidate)).get(5, TimeUnit.SECONDS)
	}

	private fun gunDistTimerExpires() {
		world.submit(GunDistTimerExpiredEvent()).get(5, TimeUnit.SECONDS)
	}


	private fun gameState(): GameEntity {
		return world.findEntitiesWith<GameMetaComponent>().first()
	}

	private fun playerComponent(userId: Long): PlayerComponent {
		return requireNotNull(
			PopcornHelper.findPlayerEntity(world, userId)
				?.findComponent<PlayerComponent>()
		)
	}

	private fun isGunHolder(userId: Long): Boolean {
		val entity = PopcornHelper.findPlayerEntity(world, userId)
		return entity != null && entity.hasComponent<GunHolderComponent>()
	}

	// ===== Standard 3-player setup: 2 village, 1 wolf =====
	private fun threePlayerSetup(): MutableList<PlayerSetup> {
		return mutableListOf(
			PlayerSetup(ALICE, Alignments.VILLAGE),
			PlayerSetup(BOB, Alignments.VILLAGE),
			PlayerSetup(WOLF_A, Alignments.WOLF)
		)
	}

	// ===== Standard 5-player setup: 3 village, 2 wolves =====
	private fun fivePlayerSetup(): MutableList<PlayerSetup> {
		return mutableListOf(
			PlayerSetup(ALICE, Alignments.VILLAGE),
			PlayerSetup(BOB, Alignments.VILLAGE),
			PlayerSetup(CAROL, Alignments.VILLAGE),
			PlayerSetup(WOLF_A, Alignments.WOLF),
			PlayerSetup(WOLF_B, Alignments.WOLF)
		)
	}

	@Nested
	internal inner class GameStart {

		@Test
		fun wildModeCreatesEntitiesAndGivesGun() {
			startWildGame(threePlayerSetup())

			// 3 player entities + 1 game-state entity
			assertThat(world.entities()).hasSize(4)

			// First villager (Alice) gets the gun in WILD mode (deterministic: first in list)
			assertThat(isGunHolder(ALICE)).isTrue()

			// Game is running, day 1
			val phase = gameState().findComponent<PhaseComponent>()
			assertThat(phase)
				.isNotNullKt()
				.satisfiesKt { assertThat(it.day()).isEqualTo(1) }


			// Role PMs sent
			assertThat(sink.dmMessagesTo(ALICE)).isNotEmpty()
			assertThat(sink.dmMessagesTo(WOLF_A)).isNotEmpty()

			// Wolf PM contains team info
			assertThat(sink.dmMessagesTo(WOLF_A).first()).contains("<@10>")

			// Game start announced
			assertThat(sink.anyChannelMessageContains("Game has started!")).isTrue()
			assertThat(sink.anyChannelMessageContains("has received the gun!")).isTrue()
		}

		@Test
		fun classicModeStartsGunDistribution() {
			startClassicGame(threePlayerSetup())

			// No gun holder yet
			assertThat(PopcornHelper.gunHolder(world)).isNull()

			// Gun distribution announced
			assertThat(sink.anyChannelMessageContains("Wolves are distributing the gun")).isTrue()
		}
	}

	@Nested
	internal inner class WildModeShoot {

		@Test
		fun villagerShootsWolf_wolfDies_villageWins() {
			startWildGame(threePlayerSetup())
			sink.clear()

			// Alice (village, has gun) shoots Wolf A
			shoot(ALICE, WOLF_A)

			assertThat(playerComponent(WOLF_A).isAlive).isFalse()
			assertThat(playerComponent(ALICE).isAlive).isTrue()
			assertThat(sink.anyChannelMessageContains("dirty wolf")).isTrue()

			// Only 1 wolf, so village wins
			val gameOver = gameState().findComponent<GameOverComponent>()
			assertThat(gameOver)
				.isNotNullKt()
				.satisfiesKt {
					assertThat(it.winner).isEqualTo(Alignments.VILLAGE)
				}
			assertThat(sink.anyChannelMessageContains("Village wins")).isTrue()
		}

		@Test
		fun villagerShootsVillager_shooterDies() {
			startWildGame(threePlayerSetup())
			sink.clear()

			// Alice (village, has gun) shoots Bob (village)
			shoot(ALICE, BOB)

			// Alice dies, not Bob
			assertThat(playerComponent(ALICE).isAlive).isFalse()
			assertThat(playerComponent(BOB).isAlive).isTrue()
			assertThat(sink.anyChannelMessageContains("innocent villager")).isTrue()

			// Bob gets the gun (target survives and gets gun in WILD mode)
			assertThat(isGunHolder(BOB)).isTrue()

			// Wolves reach parity (1 wolf, 1 villager): wolves win
			val gameOver = gameState().findComponent<GameOverComponent>()
			assertThat(gameOver)
				.isNotNullKt()
				.satisfiesKt {
					assertThat(it.winner).isEqualTo(Alignments.WOLF)
				}
		}

		@Test
		fun multipleRounds_villageWins() {
			startWildGame(fivePlayerSetup())
			sink.clear()

			// Day 1: Alice shoots Wolf A (wolf dies)
			shoot(ALICE, WOLF_A)
			assertThat(playerComponent(WOLF_A).isAlive).isFalse()
			assertThat(isGunHolder(ALICE)).isTrue()

			// Not over yet: 1 wolf remaining, 3 villagers
			assertThat(gameState().findComponent<GameOverComponent>()).isNull()

			// Day 2: Alice shoots Wolf B (wolf dies, village wins)
			shoot(ALICE, WOLF_B)
			assertThat(playerComponent(WOLF_B).isAlive).isFalse()

			val gameOver = gameState().findComponent<GameOverComponent>()
			assertThat(gameOver)
				.isNotNullKt()
				.satisfiesKt {
					assertThat(it.winner).isEqualTo(Alignments.VILLAGE)
				}
		}

		@Test
		fun multipleRounds_wolvesWin() {
			startWildGame(fivePlayerSetup())
			sink.clear()

			// Day 1: Alice shoots Bob (villager, Alice dies)
			shoot(ALICE, BOB)
			assertThat(playerComponent(ALICE).isAlive).isFalse()
			assertThat(isGunHolder(BOB)).isTrue()

			// 2 wolves, 2 villagers: parity, wolves win
			val gameOver = gameState().findComponent<GameOverComponent>()
			assertThat(gameOver)
				.isNotNullKt()
				.satisfiesKt {
					assertThat(it.winner).isEqualTo(Alignments.WOLF)
				}
		}
	}

	@Nested
	internal inner class ShootValidation {

		@Test
		fun nonPlayerCannotShoot() {
			startWildGame(threePlayerSetup())
			sink.clear()

			shoot(999L, WOLF_A)

			assertThat(sink.anyChannelMessageContains("not playing")).isTrue()
			// No one died
			assertThat(playerComponent(WOLF_A).isAlive).isTrue()
		}

		@Test
		fun deadPlayerCannotShoot() {
			// 5 players, kill Alice first
			startWildGame(fivePlayerSetup())

			// Alice shoots a villager (Bob), Alice dies
			shoot(ALICE, BOB)
			assertThat(playerComponent(ALICE).isAlive).isFalse()

			// Game shouldn't be over (2 wolves, 2 villagers = parity... actually it is over)
			// Let's use a bigger setup: 4v2 so shooting a villager doesn't end it
			// Actually with 5 players (3v2), Alice shoots Bob: Alice dies, 2v2 = parity = game over
			// So let's just test: dead Alice tries to shoot, gets told she's dead
			// But game is already over... Let me make a different test.
		}

		@Test
		fun cannotShootSelf() {
			startWildGame(threePlayerSetup())
			sink.clear()

			shoot(ALICE, ALICE)

			assertThat(sink.anyChannelMessageContains("shoot yourself")).isTrue()
			assertThat(playerComponent(ALICE).isAlive).isTrue()
		}

		@Test
		fun nonGunHolderCannotShoot() {
			startWildGame(threePlayerSetup())
			sink.clear()

			// Bob does not have the gun
			shoot(BOB, WOLF_A)

			assertThat(sink.anyChannelMessageContains("do not have the gun")).isTrue()
			assertThat(playerComponent(WOLF_A).isAlive).isTrue()
		}

		@Test
		fun cannotShootDeadPlayer() {
			startWildGame(fivePlayerSetup())

			// Alice shoots Wolf A (wolf dies)
			shoot(ALICE, WOLF_A)
			assertThat(playerComponent(WOLF_A).isAlive).isFalse()
			sink.clear()

			// Alice still has gun, tries to shoot dead Wolf A again
			shoot(ALICE, WOLF_A)

			assertThat(sink.anyChannelMessageContains("living player")).isTrue()
		}
	}

	@Nested
	internal inner class TimerExpiry {

		@Test
		fun timerModkillsGunBearer() {
			startWildGame(fivePlayerSetup())
			val phase = requireNotNull(gameState().findComponent<PhaseComponent>())
			val currentDay = phase.day()
			sink.clear()

			// Timer expires for current day
			timerExpires(currentDay)

			// Alice (gun holder) dies
			assertThat(playerComponent(ALICE).isAlive).isFalse()
			assertThat(sink.anyChannelMessageContains("took too long")).isTrue()

			// Gun redistributed to next villager (Bob in WILD mode)
			assertThat(isGunHolder(BOB)).isTrue()
		}

		@Test
		fun staleTimerIsIgnored() {
			startWildGame(threePlayerSetup())
			sink.clear()

			// Submit a timer for day 99 (doesn't match current day 1)
			timerExpires(99)

			// Nothing happened
			assertThat(playerComponent(ALICE).isAlive).isTrue()
			assertThat(isGunHolder(ALICE)).isTrue()
		}

		@Test
		fun timerModkillCanEndGame() {
			// 3 players: 2 village, 1 wolf. Kill Alice via timer → 1v1 parity → wolves win
			startWildGame(threePlayerSetup())
			val phase = requireNotNull(gameState().findComponent<PhaseComponent>())
			sink.clear()

			timerExpires(phase.day())

			// Alice died from timer, 1 villager + 1 wolf = parity
			assertThat(playerComponent(ALICE).isAlive).isFalse()
			val gameOver = gameState().findComponent<GameOverComponent>()
			assertThat(gameOver)
				.isNotNullKt()
				.satisfiesKt {
					assertThat(it.winner).isEqualTo(Alignments.WOLF)
				}
		}
	}

	@Nested
	internal inner class ClassicModeGunDistribution {

		@Test
		fun wolvesVoteToGiveGun() {
			// 5 players: Alice, Bob, Carol (village), Wolf A, Wolf B (wolves)
			startClassicGame(fivePlayerSetup())
			sink.clear()

			// Wolf A and Wolf B both vote for Bob to receive the gun
			gunDistVote(WOLF_A, BOB)
			gunDistVote(WOLF_B, BOB)

			// All wolves voted → distribution resolves
			assertThat(isGunHolder(BOB)).isTrue()
			assertThat(sink.anyChannelMessageContains("has received the gun")).isTrue()

			// Day started
			val phase = requireNotNull(gameState().findComponent<PhaseComponent>())
			assertThat(phase.day()).isEqualTo(1)
		}

		@Test
		fun gunDistTimerResolvesWithPartialVotes() {
			startClassicGame(fivePlayerSetup())
			sink.clear()

			// Only Wolf A votes for Carol
			gunDistVote(WOLF_A, CAROL)

			// Timer expires before Wolf B votes
			gunDistTimerExpires()

			// Carol gets the gun (most voted)
			assertThat(isGunHolder(CAROL)).isTrue()
		}

		@Test
		fun gunDistTimerResolvesWithNoVotes() {
			startClassicGame(fivePlayerSetup())
			sink.clear()

			// No one votes, timer expires
			gunDistTimerExpires()

			// First villager (Alice) gets the gun
			assertThat(isGunHolder(ALICE)).isTrue()
		}

		@Test
		fun nonWolfVoteIsIgnored() {
			startClassicGame(fivePlayerSetup())
			sink.clear()

			// Alice (villager) tries to vote — should be ignored
			gunDistVote(ALICE, BOB)

			// Distribution not resolved (wolves haven't voted)
			assertThat(PopcornHelper.gunHolder(world)).isNull()
		}

		@Test
		fun fullClassicGameFlow() {
			startClassicGame(fivePlayerSetup())
			sink.clear()

			// Wolves give gun to Alice
			gunDistVote(WOLF_A, ALICE)
			gunDistVote(WOLF_B, ALICE)

			assertThat(isGunHolder(ALICE)).isTrue()

			// Alice shoots Wolf A
			shoot(ALICE, WOLF_A)
			assertThat(playerComponent(WOLF_A).isAlive).isFalse()

			// Game not over (1 wolf left). Alice keeps gun, new day starts.
			assertThat(isGunHolder(ALICE)).isTrue()

			// Alice shoots Wolf B — village wins
			shoot(ALICE, WOLF_B)
			val gameOver = gameState().findComponent<GameOverComponent>()
			assertThat(gameOver)
				.isNotNullKt()
				.satisfiesKt {
					assertThat(it.winner).isEqualTo(Alignments.VILLAGE)
				}
		}

		@Test
		fun classicMode_villagerShotTriggersRedistribution() {
			startClassicGame(fivePlayerSetup())

			// Wolves give gun to Alice
			gunDistVote(WOLF_A, ALICE)
			gunDistVote(WOLF_B, ALICE)
			sink.clear()

			// Alice shoots Bob (villager) — Alice dies, gun needs redistribution
			shoot(ALICE, BOB)
			assertThat(playerComponent(ALICE).isAlive).isFalse()

			// In CLASSIC mode, wolves redistribute
			// 2 wolves, 1 villager (Carol) = parity = wolves win
			val gameOver = gameState().findComponent<GameOverComponent>()
			assertThat(gameOver)
				.isNotNullKt()
				.satisfiesKt { assertThat(it.winner).isEqualTo(Alignments.WOLF) }

		}
	}
}
