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
package space.npstr.wolfia.domain.setup.lastactive

import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.count
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.gen.Tables

@Repository
class LastActiveRepository(
	private val clock: Clock,
	private val jooq: DSLContext,
	private val eventPublisher: ApplicationEventPublisher,
) {

	data class UserBecameInactive(
		val userId: Long,
	)

	@Scheduled(fixedDelay = 1, timeUnit = SECONDS, initialDelay = 1)
	internal fun expire() {
		val now = clock.millis()
		jooq.transactionResult { config ->
			DSL.using(config)
				.deleteFrom(Tables.LAST_ACTIVE)
				.where(Tables.LAST_ACTIVE.EXPIRES.lessThan(now))
				.returning()
				.fetch(Tables.LAST_ACTIVE.USER_ID)
		}.forEach { eventPublisher.publishEvent(UserBecameInactive(it)) }
	}

	fun recordActivity(userId: Long, timeout: Duration) {
		recordActivities(setOf(userId), timeout)
	}

	fun recordActivities(userIds: Set<Long>, timeout: Duration) {
		if (userIds.isEmpty()) return
		val now = clock.instant()
		val expires = now.plus(timeout)

		jooq.transaction { config ->
			val context = DSL.using(config)
			val queries = userIds.map { userId ->
				context
					.insertInto(Tables.LAST_ACTIVE)
					.columns(Tables.LAST_ACTIVE.USER_ID, Tables.LAST_ACTIVE.TIMESTAMP, Tables.LAST_ACTIVE.EXPIRES)
					.values(userId, now.toEpochMilli(), expires.toEpochMilli())
					.onDuplicateKeyUpdate()
					.set(Tables.LAST_ACTIVE.TIMESTAMP, now.toEpochMilli())
					.set(Tables.LAST_ACTIVE.EXPIRES, expires.toEpochMilli())
			}
			context.batch(queries).execute()
		}
	}

	fun wasActiveRecently(userId: Long): Boolean {
		val now = clock.millis()
		return jooq.select(count())
			.from(Tables.LAST_ACTIVE)
			.where(Tables.LAST_ACTIVE.USER_ID.eq(userId))
			.and(Tables.LAST_ACTIVE.EXPIRES.greaterThan(now))
			.fetchSingle()
			.get(count()) > 0
	}
}
