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
package space.npstr.wolfia.domain.settings

import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.Database
import space.npstr.wolfia.db.gen.Tables

@Repository
internal class GuildSettingsRepository(
	private val database: Database,
) {

	fun findOne(guildId: Long): GuildSettings? {
		return database.jooq
			.selectFrom(Tables.GUILD_SETTINGS)
			.where(Tables.GUILD_SETTINGS.GUILD_ID.eq(guildId))
			.fetchOneInto(GuildSettings::class.java)
	}

	fun findOneOrDefault(guildId: Long): GuildSettings {
		return database.jooq
			.insertInto(Tables.GUILD_SETTINGS)
			.columns(Tables.GUILD_SETTINGS.GUILD_ID)
			.values(guildId)
			.onDuplicateKeyUpdate()
			.set(Tables.GUILD_SETTINGS.GUILD_ID, guildId)
			.returning()
			.fetchSingleInto(GuildSettings::class.java)
	}

	fun set(guildId: Long, name: String, iconId: String?): GuildSettings {
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.insertInto(Tables.GUILD_SETTINGS)
				.columns(Tables.GUILD_SETTINGS.GUILD_ID, Tables.GUILD_SETTINGS.NAME, Tables.GUILD_SETTINGS.ICON_ID)
				.values(guildId, name, iconId)
				.onDuplicateKeyUpdate()
				.set(Tables.GUILD_SETTINGS.NAME, name)
				.set(Tables.GUILD_SETTINGS.ICON_ID, iconId)
				.returning()
				.fetchSingleInto(GuildSettings::class.java)
		}
	}
}
