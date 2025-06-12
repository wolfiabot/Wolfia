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
package space.npstr.wolfia.domain.ban

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.gen.Tables
import space.npstr.wolfia.game.definitions.Scope

@Repository
internal class BanRepository(
	private val jooq: DSLContext,
) {

	fun findOne(userId: Long, scope: Scope): Ban? {
		return jooq
			.selectFrom(Tables.DISCORD_USER)
			.where(Tables.DISCORD_USER.USER_ID.eq(userId).and(Tables.DISCORD_USER.BAN.eq(scope.name)))
			.fetchOneInto(Ban::class.java)
	}

	fun setScope(userId: Long, scope: Scope): Ban {
		return jooq.transactionResult { config ->
			config.dsl()
				.insertInto(Tables.DISCORD_USER)
				.columns(Tables.DISCORD_USER.USER_ID, Tables.DISCORD_USER.BAN)
				.values(userId, scope.name)
				.onDuplicateKeyUpdate()
				.set(Tables.DISCORD_USER.BAN, scope.name)
				.returning()
				.fetchSingleInto(Ban::class.java)
		}
	}

	fun findByScope(scope: Scope): List<Ban> {
		return jooq
			.selectFrom(Tables.DISCORD_USER)
			.where(Tables.DISCORD_USER.BAN.eq(scope.name))
			.fetchInto(Ban::class.java)
	}
}
