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
package space.npstr.wolfia.domain.oauth2

import java.time.Duration
import java.time.Instant
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.Database
import space.npstr.wolfia.db.gen.Tables

@Repository
internal class OAuth2Repository(
	private val database: Database,
) {

	fun findOne(userId: Long): OAuth2Data? {
		return database.jooq()
			.selectFrom(Tables.OAUTH2)
			.where(Tables.OAUTH2.USER_ID.eq(userId))
			.fetchOneInto(OAuth2Data::class.java)
	}

	fun findAllExpiringIn(duration: Duration): List<OAuth2Data> {
		val expiresOn = Instant.now().plusSeconds(duration.toSeconds())
		return database.jooq()
			.selectFrom(Tables.OAUTH2)
			.where(Tables.OAUTH2.EXPIRES.lessThan(expiresOn))
			.fetchInto(OAuth2Data::class.java)
	}

	fun delete(userId: Long): Int {
		return database.jooq().transactionResult { config ->
			config.dsl()
				.deleteFrom(Tables.OAUTH2)
				.where(Tables.OAUTH2.USER_ID.eq(userId))
				.execute()
		}
	}

	fun save(data: OAuth2Data): OAuth2Data {
		val scopes = data.scopes().toTypedArray()
		return database.jooq().transactionResult { config ->
			config.dsl()
				.insertInto(Tables.OAUTH2)
				.columns(Tables.OAUTH2.USER_ID, Tables.OAUTH2.ACCESS_TOKEN, Tables.OAUTH2.EXPIRES, Tables.OAUTH2.REFRESH_TOKEN, Tables.OAUTH2.SCOPES)
				.values(data.userId(), data.accessToken(), data.expires(), data.refreshToken(), scopes)
				.onDuplicateKeyUpdate()
				.set(Tables.OAUTH2.ACCESS_TOKEN, data.accessToken())
				.set(Tables.OAUTH2.EXPIRES, data.expires())
				.set(Tables.OAUTH2.REFRESH_TOKEN, data.refreshToken())
				.set(Tables.OAUTH2.SCOPES, scopes)
				.returning()
				.fetchSingle()
				.into(OAuth2Data::class.java)
		}
	}
}
