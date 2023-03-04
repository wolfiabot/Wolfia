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
package space.npstr.wolfia.domain.setup

import java.time.Duration
import org.jooq.Field
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.Database
import space.npstr.wolfia.db.ExtendedPostgresDSL
import space.npstr.wolfia.db.gen.Tables
import space.npstr.wolfia.game.GameInfo.GameMode
import space.npstr.wolfia.game.definitions.Games

@Repository
internal class GameSetupRepository(
	private val database: Database,
) {

	fun findOne(channelId: Long): GameSetup? {
		return database.jooq
			.selectFrom(Tables.GAME_SETUP)
			.where(Tables.GAME_SETUP.CHANNEL_ID.eq(channelId))
			.fetchOneInto(GameSetup::class.java)
	}

	//this works since we dont commit the transaction
	fun findOneOrDefault(channelId: Long): GameSetup {
		return database.jooq
			.insertInto(Tables.GAME_SETUP)
			.columns(Tables.GAME_SETUP.CHANNEL_ID)
			.values(channelId)
			.onDuplicateKeyUpdate() // cant ignore, otherwise returning() will be empty on conflict
			.set(Tables.GAME_SETUP.CHANNEL_ID, channelId)
			.returning()
			.fetchSingleInto(GameSetup::class.java)
	}

	fun findAutoOutSetupsWhereUserIsInned(userId: Long): List<GameSetup> {
		return database.jooq
			.select(
				Tables.GAME_SETUP.CHANNEL_ID,
				Tables.GAME_SETUP.INNED_USERS,
				Tables.GAME_SETUP.GAME,
				Tables.GAME_SETUP.MODE,
				Tables.GAME_SETUP.DAY_LENGTH
			)
			.from(Tables.GAME_SETUP)
			.join(Tables.CHANNEL_SETTINGS).on(Tables.GAME_SETUP.CHANNEL_ID.eq(Tables.CHANNEL_SETTINGS.CHANNEL_ID))
			.where(Tables.CHANNEL_SETTINGS.AUTO_OUT.isTrue)
			.and(Tables.GAME_SETUP.INNED_USERS.contains(arrayOf(userId)))
			.fetchInto(GameSetup::class.java)
	}

	fun setGame(channelId: Long, game: Games): GameSetup {
		return set(channelId, Tables.GAME_SETUP.GAME, game.name)
	}

	fun setMode(channelId: Long, mode: GameMode): GameSetup {
		return set(channelId, Tables.GAME_SETUP.MODE, mode.name)
	}

	fun setDayLength(channelId: Long, dayLength: Duration): GameSetup {
		return set(channelId, Tables.GAME_SETUP.DAY_LENGTH, dayLength.toMillis())
	}

	fun inUsers(channelId: Long, userIds: Collection<Long>): GameSetup {
		val userIdsArray = userIds.toTypedArray()
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.insertInto(Tables.GAME_SETUP)
				.columns(Tables.GAME_SETUP.CHANNEL_ID, Tables.GAME_SETUP.INNED_USERS)
				.values(channelId, userIdsArray)
				.onDuplicateKeyUpdate()
				.set(Tables.GAME_SETUP.INNED_USERS, ExtendedPostgresDSL.arrayAppendDistinct(Tables.GAME_SETUP.INNED_USERS, *userIdsArray))
				.returning()
				.fetchSingleInto(GameSetup::class.java)
		}
	}

	fun outUsers(channelId: Long, userIds: Collection<Long>): GameSetup {
		val userIdsArray = userIds.toTypedArray()
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.insertInto(Tables.GAME_SETUP)
				.columns(Tables.GAME_SETUP.CHANNEL_ID, Tables.GAME_SETUP.INNED_USERS)
				.values(channelId, arrayOf())
				.onDuplicateKeyUpdate()
				.set(Tables.GAME_SETUP.INNED_USERS, ExtendedPostgresDSL.arrayDiff(Tables.GAME_SETUP.INNED_USERS, *userIdsArray))
				.returning()
				.fetchSingleInto(GameSetup::class.java)
		}
	}

	fun delete(channelId: Long): Int {
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.deleteFrom(Tables.GAME_SETUP)
				.where(Tables.GAME_SETUP.CHANNEL_ID.eq(channelId))
				.execute()
		}
	}

	private fun <F> set(channelId: Long, field: Field<F>, value: F): GameSetup {
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.insertInto(Tables.GAME_SETUP)
				.columns(Tables.GAME_SETUP.CHANNEL_ID, field)
				.values(channelId, value)
				.onDuplicateKeyUpdate()
				.set(field, value)
				.returning()
				.fetchSingleInto(GameSetup::class.java)
		}
	}
}
