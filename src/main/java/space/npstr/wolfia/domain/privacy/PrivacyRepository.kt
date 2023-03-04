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
package space.npstr.wolfia.domain.privacy

import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.Database
import space.npstr.wolfia.db.gen.Tables

@Repository
internal class PrivacyRepository(
	private val database: Database,
) {

	fun findOne(userId: Long): Privacy? {
		return database.jooq()
			.selectFrom(Tables.DISCORD_USER)
			.where(Tables.DISCORD_USER.USER_ID.eq(userId))
			.fetchOneInto(Privacy::class.java)
	}

	fun setProcessData(userId: Long, processData: Boolean): Privacy {
		return database.jooq().transactionResult { config ->
			config.dsl()
				.insertInto(Tables.DISCORD_USER)
				.columns(Tables.DISCORD_USER.USER_ID, Tables.DISCORD_USER.PROCESS_DATA)
				.values(userId, processData)
				.onDuplicateKeyUpdate()
				.set(Tables.DISCORD_USER.PROCESS_DATA, processData)
				.returning()
				.fetchSingleInto(Privacy::class.java)
		}
	}

	fun findAllDeniedProcessData(): List<Privacy> {
		return database.jooq()
			.selectFrom(Tables.DISCORD_USER)
			.where(Tables.DISCORD_USER.PROCESS_DATA.eq(false))
			.fetchInto(Privacy::class.java)
	}
}
