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

package space.npstr.wolfia.domain.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.lang.Nullable;

import static org.immutables.value.Value.Immutable;

/**
 * Guild fetched via OAuth2, see https://discord.com/developers/docs/resources/user#get-current-user-guilds-example-partial-guild
 */
@Immutable
@JsonSerialize(as = ImmutablePartialGuild.class)
@JsonDeserialize(as = ImmutablePartialGuild.class)
public interface PartialGuild {

    @JsonSerialize(using = ToStringSerializer.class)
    long id();

    String name();

    @Nullable
    String icon();

    @JsonProperty("owner")
    boolean isOwner();

    @JsonSerialize(using = ToStringSerializer.class)
    long permissions();
}
