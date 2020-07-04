/*
 * Copyright (C) 2016-2020 the original author or authors
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

package space.npstr.wolfia.domain.settings;

import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.AsyncDbWrapper;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static space.npstr.wolfia.db.gen.Tables.GUILD_SETTINGS;

@Repository
public class GuildSettingsRepository {

    private final AsyncDbWrapper wrapper;

    public GuildSettingsRepository(AsyncDbWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @CheckReturnValue
    public CompletionStage<Optional<GuildSettings>> findOne(long guildId) {
        return this.wrapper.jooq(dsl -> dsl
                .selectFrom(GUILD_SETTINGS)
                .where(GUILD_SETTINGS.GUILD_ID.eq(guildId))
                .fetchOptionalInto(GuildSettings.class)
        );
    }

    @CheckReturnValue
    public CompletionStage<GuildSettings> findOneOrDefault(long guildId) {
        return this.wrapper.jooq(dsl -> dsl
                .insertInto(GUILD_SETTINGS)
                .columns(GUILD_SETTINGS.GUILD_ID)
                .values(guildId)
                .onDuplicateKeyUpdate()
                .set(GUILD_SETTINGS.GUILD_ID, guildId)
                .returning()
                .fetchOne()
                .into(GuildSettings.class)
        );
    }

    @CheckReturnValue
    public CompletionStage<GuildSettings> set(long guildId, String name, String iconId) {
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(GUILD_SETTINGS)
                .columns(GUILD_SETTINGS.GUILD_ID, GUILD_SETTINGS.NAME, GUILD_SETTINGS.ICON_ID)
                .values(guildId, name, iconId)
                .onDuplicateKeyUpdate()
                .set(GUILD_SETTINGS.NAME, name)
                .set(GUILD_SETTINGS.ICON_ID, iconId)
                .returning()
                .fetchOne()
                .into(GuildSettings.class)
        ));
    }
}
