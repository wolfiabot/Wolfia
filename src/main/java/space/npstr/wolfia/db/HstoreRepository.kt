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
package space.npstr.wolfia.db

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.gen.Tables
import space.npstr.wolfia.db.gen.tables.records.HstorexRecord

@Repository
class HstoreRepository(
	private val jooq: DSLContext,
) {

	/**
	 * @return the default value if either the hstore or the key inside the hstore doesnt exist.
	 */
	fun get(name: String, key: String, defaultValue: String): String {
		return jooq
			.select(Tables.HSTOREX.HSTOREX_)
			.from(Tables.HSTOREX)
			.where(Tables.HSTOREX.NAME.eq(name))
			.fetchOptional(Tables.HSTOREX.HSTOREX_)
			.map { map -> map.getOrDefault(key, defaultValue) }
			.orElse(defaultValue)
	}

	fun set(name: String, key: String, value: String): HstorexRecord {
		val toAppend = HashMap(java.util.Map.of(key, value))
		return set(name, toAppend)
	}

	fun set(name: String, toAppend: Map<String, String>): HstorexRecord {
		val map = HashMap(toAppend)
		return jooq.transactionResult { config ->
			config.dsl()
				.insertInto(Tables.HSTOREX)
				.columns(Tables.HSTOREX.NAME, Tables.HSTOREX.HSTOREX_)
				.values(name, map)
				.onDuplicateKeyUpdate()
				.set(
					Tables.HSTOREX.HSTOREX_,
					concat(Tables.HSTOREX.HSTOREX_, DSL.`val`(map, Tables.HSTOREX.HSTOREX_.dataType))
				)
				.returning()
				.fetchSingle()
		}
	}

	companion object {
		//source: https://stackoverflow.com/questions/27864026/update-hstore-fields-using-jooq
		private fun concat(
			f1: Field<HashMap<String, String>>,
			f2: Field<HashMap<String, String>>
		): Field<HashMap<String, String>> {
			return DSL.field("{0} || {1}", f1.dataType, f1, f2)
		}
	}
}
