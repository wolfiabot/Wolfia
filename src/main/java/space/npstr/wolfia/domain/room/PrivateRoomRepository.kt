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
package space.npstr.wolfia.domain.room

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.gen.Tables

@Repository
internal class PrivateRoomRepository(
	private val jooq: DSLContext,
) {

	fun findOneByGuildId(guildId: Long): PrivateRoom? {
		return jooq
			.selectFrom(Tables.PRIVATE_ROOM)
			.where(Tables.PRIVATE_ROOM.GUILD_ID.eq(guildId))
			.fetchOneInto(PrivateRoom::class.java)
	}

	fun findAll(): List<PrivateRoom> {
		return jooq
			.selectFrom(Tables.PRIVATE_ROOM)
			.orderBy(Tables.PRIVATE_ROOM.NR.asc())
			.fetchInto(PrivateRoom::class.java)
	}

	// inspired by the 2. solution from https://www.eidias.com/blog/2012/1/16/finding-gaps-in-a-sequence-of-identifier-values-of
	// this can probably be written in a single query but why bother, race conditions are not expected for registering
	fun insert(guildId: Long): PrivateRoom? {
		val firstFreeNumber = getFirstFreeNumber()
		return jooq.transactionResult { config ->
			config.dsl()
				.insertInto(Tables.PRIVATE_ROOM)
				.columns(Tables.PRIVATE_ROOM.GUILD_ID, Tables.PRIVATE_ROOM.NR)
				.values(guildId, firstFreeNumber)
				.onDuplicateKeyIgnore()
				.returning()
				.fetchOneInto(PrivateRoom::class.java)
		}
	}

	private fun getFirstFreeNumber(): Int {
		val numberOneExists = numberOneExists()
		if (!numberOneExists) {
			return 1
		}

		val a = Tables.PRIVATE_ROOM.`as`("a")
		val b = Tables.PRIVATE_ROOM.`as`("b")
		return jooq
			.select(a.NR.add(1))
			.from(a)
			.whereNotExists(
				jooq
					.selectFrom(b)
					.where(a.NR.add(1).eq(b.NR))
			)
			.orderBy(a.NR.asc())
			.limit(1)
			.fetchSingle()
			.component1()
	}

	private fun numberOneExists(): Boolean {
		return jooq
			.select(DSL.value(1))
			.from(Tables.PRIVATE_ROOM)
			.where(Tables.PRIVATE_ROOM.NR.eq(1))
			.fetchOptional()
			.isPresent
	}
}
