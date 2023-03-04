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

import org.jooq.Field
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import space.npstr.wolfia.db.Database
import space.npstr.wolfia.db.ExtendedPostgresDSL
import space.npstr.wolfia.db.gen.Tables

/**
 * Saves settings on a per channel basis. The difference to [space.npstr.wolfia.db.gen.tables.records.GameSetupRecord]
 * is that this record contains technical Discord stuff, while [space.npstr.wolfia.db.gen.tables.records.GameSetupRecord]
 * should contain purely game related stuff.
 */
@Repository
class ChannelSettingsRepository(
	private val database: Database,
) {

	fun findOne(channelId: Long): ChannelSettings? {
		return database.jooq
			.selectFrom(Tables.CHANNEL_SETTINGS)
			.where(Tables.CHANNEL_SETTINGS.CHANNEL_ID.eq(channelId))
			.fetchOneInto(ChannelSettings::class.java)
	}

	//this works since we dont commit the transaction
	fun findOneOrDefault(channelId: Long): ChannelSettings {
		return database.jooq
			.insertInto(Tables.CHANNEL_SETTINGS)
			.columns(Tables.CHANNEL_SETTINGS.CHANNEL_ID)
			.values(channelId)
			.onDuplicateKeyUpdate() // cant ignore, otherwise returning() will be empty on conflict
			.set(Tables.CHANNEL_SETTINGS.CHANNEL_ID, channelId)
			.returning()
			.fetchSingle()
			.into(ChannelSettings::class.java)
	}

	fun findOrDefault(channelIds: Collection<Long>): List<ChannelSettings> {
		return if (channelIds.isEmpty()) {
			listOf()
		} else {
			var insert = database.jooq.insertInto(Tables.CHANNEL_SETTINGS)
				.columns(Tables.CHANNEL_SETTINGS.CHANNEL_ID)
			for (channelId in channelIds) {
				insert = insert.values(channelId)
			}
			// See https://github.com/jOOQ/jOOQ/issues/5214#issuecomment-213574749
			val excluded = Tables.CHANNEL_SETTINGS.`as`("excluded")
			insert
				.onDuplicateKeyUpdate() // cant ignore, otherwise returning() will be empty on conflict
				.set(Tables.CHANNEL_SETTINGS.CHANNEL_ID, excluded.CHANNEL_ID)
				.returning()
				.fetch()
				.into(ChannelSettings::class.java)
		}
	}

	fun setAccessRoleId(channelId: Long, accessRoleId: Long): ChannelSettings {
		return set(channelId, Tables.CHANNEL_SETTINGS.ACCESS_ROLE_ID, accessRoleId)
	}

	fun setAutoOut(channelId: Long, autoOut: Boolean): ChannelSettings {
		return set(channelId, Tables.CHANNEL_SETTINGS.AUTO_OUT, autoOut)
	}

	fun setGameChannel(channelId: Long, isGameChannel: Boolean): ChannelSettings {
		return set(channelId, Tables.CHANNEL_SETTINGS.IS_GAME_CHANNEL, isGameChannel)
	}

	fun setTagCooldown(channelId: Long, tagCooldown: Long): ChannelSettings {
		return set(channelId, Tables.CHANNEL_SETTINGS.TAG_COOLDOWN, tagCooldown)
	}

	fun setTagLastUsed(channelId: Long, lastUsed: Long): ChannelSettings {
		return set(channelId, Tables.CHANNEL_SETTINGS.TAG_LAST_USED, lastUsed)
	}

	fun addTags(channelId: Long, tags: Collection<Long>): ChannelSettings {
		val tagArray = tags.toTypedArray()
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.insertInto(Tables.CHANNEL_SETTINGS)
				.columns(Tables.CHANNEL_SETTINGS.CHANNEL_ID, Tables.CHANNEL_SETTINGS.TAGS)
				.values(channelId, tagArray)
				.onDuplicateKeyUpdate()
				.set(Tables.CHANNEL_SETTINGS.TAGS, ExtendedPostgresDSL.arrayAppendDistinct(Tables.CHANNEL_SETTINGS.TAGS, *tagArray))
				.returning()
				.fetchSingleInto(ChannelSettings::class.java)
		}
	}

	fun removeTags(channelId: Long, tags: Collection<Long>): ChannelSettings {
		val tagArray = tags.toTypedArray()
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.insertInto(Tables.CHANNEL_SETTINGS)
				.columns(Tables.CHANNEL_SETTINGS.CHANNEL_ID, Tables.CHANNEL_SETTINGS.TAGS)
				.values(channelId, arrayOf())
				.onDuplicateKeyUpdate()
				.set(Tables.CHANNEL_SETTINGS.TAGS, ExtendedPostgresDSL.arrayDiff(Tables.CHANNEL_SETTINGS.TAGS, *tagArray))
				.returning()
				.fetchSingleInto(ChannelSettings::class.java)
		}
	}

	fun delete(channelId: Long): Int {
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.deleteFrom(Tables.CHANNEL_SETTINGS)
				.where(Tables.CHANNEL_SETTINGS.CHANNEL_ID.eq(channelId))
				.execute()
		}
	}

	private operator fun <F> set(channelId: Long, field: Field<F>, value: F): ChannelSettings {
		return database.jooq.transactionResult { config ->
			DSL.using(config)
				.insertInto(Tables.CHANNEL_SETTINGS)
				.columns(Tables.CHANNEL_SETTINGS.CHANNEL_ID, field)
				.values(channelId, value)
				.onDuplicateKeyUpdate()
				.set(field, value)
				.returning()
				.fetchSingleInto(ChannelSettings::class.java)
		}
	}
}
