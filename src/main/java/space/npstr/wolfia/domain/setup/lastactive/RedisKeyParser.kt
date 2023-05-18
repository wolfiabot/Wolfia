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
package space.npstr.wolfia.domain.setup.lastactive

import java.util.regex.Pattern

internal class RedisKeyParser {

	private val prefix = "user"
	private val suffix = "last_active"
	private val userKeyFormat = "$prefix:%s:$suffix"

	// https://regex101.com/r/UXjQlB/1
	private val userKeyPattern = Pattern.compile("$prefix:(\\d+):$suffix")


	fun toKey(userId: Long): String {
		return String.format(userKeyFormat, userId)
	}

	fun fromKey(key: String): Long? {
		val matcher = userKeyPattern.matcher(key)
		return if (!matcher.matches()) {
			null
		} else try {
			matcher.group(1).toLong()
		} catch (e: NumberFormatException) {
			null
		}
	}
}
