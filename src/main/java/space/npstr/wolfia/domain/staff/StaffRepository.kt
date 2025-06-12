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
package space.npstr.wolfia.domain.staff

import java.net.URI
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.gen.Tables
import space.npstr.wolfia.db.gen.enums.StaffFunction
import space.npstr.wolfia.db.gen.tables.records.StaffMemberRecord

/**
 * Persist staff member data.
 */
@Repository
internal class StaffRepository(
	private val jooq: DSLContext,
) {

	fun getStaffMember(userId: Long): StaffMemberRecord? {
		return jooq
			.selectFrom(Tables.STAFF_MEMBER)
			.where(Tables.STAFF_MEMBER.USER_ID.eq(userId))
			.fetchOne()
	}

	fun fetchAllStaffMembers(): List<StaffMemberRecord> {
		return jooq
			.selectFrom(Tables.STAFF_MEMBER)
			.fetch()
	}

	fun updateOrCreateStaffMemberFunction(userId: Long, staffFunction: StaffFunction): StaffMemberRecord {
		return jooq.transactionResult { config ->
			config.dsl()
				.insertInto(Tables.STAFF_MEMBER)
				.columns(Tables.STAFF_MEMBER.USER_ID, Tables.STAFF_MEMBER.FUNCTION)
				.values(userId, staffFunction)
				.onDuplicateKeyUpdate()
				.set(Tables.STAFF_MEMBER.FUNCTION, staffFunction)
				.returning()
				.fetchSingle()
		}
	}

	fun updateSlogan(userId: Long, slogan: String?): StaffMemberRecord? {
		return jooq.transactionResult { config ->
			config.dsl()
				.update(Tables.STAFF_MEMBER)
				.set(Tables.STAFF_MEMBER.SLOGAN, slogan)
				.where(Tables.STAFF_MEMBER.USER_ID.eq(userId))
				.returning()
				.fetchOne()
		}
	}

	fun updateLink(userId: Long, link: URI?): StaffMemberRecord? {
		return jooq.transactionResult { config ->
			config.dsl()
				.update(Tables.STAFF_MEMBER)
				.set(Tables.STAFF_MEMBER.LINK, link)
				.where(Tables.STAFF_MEMBER.USER_ID.eq(userId))
				.returning()
				.fetchOne()
		}
	}

	fun updateEnabled(userId: Long, enabled: Boolean): StaffMemberRecord? {
		return jooq.transactionResult { config ->
			config.dsl()
				.update(Tables.STAFF_MEMBER)
				.set(Tables.STAFF_MEMBER.ENABLED, enabled)
				.where(Tables.STAFF_MEMBER.USER_ID.eq(userId))
				.returning()
				.fetchOne()
		}
	}

	fun updateAllActive(activeStaff: Collection<Long>) {
		jooq.transaction { config ->
			config.dsl()
				.update(Tables.STAFF_MEMBER)
				.set(Tables.STAFF_MEMBER.ACTIVE, true)
				.where(Tables.STAFF_MEMBER.USER_ID.`in`(activeStaff))
				.execute()
			config.dsl()
				.update(Tables.STAFF_MEMBER)
				.set(Tables.STAFF_MEMBER.ACTIVE, false)
				.where(DSL.not(Tables.STAFF_MEMBER.USER_ID.`in`(activeStaff)))
				.execute()
		}
	}
}
