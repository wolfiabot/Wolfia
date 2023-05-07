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

package space.npstr.wolfia.domain.settings;

import java.beans.ConstructorProperties;
import java.util.Optional;
import org.springframework.lang.Nullable;

public class GuildSettings {

    private static final String DEFAULT_NAME = "Unknown Guild";

    private final long guildId;
    private final Optional<String> name;
    private final Optional<String> iconId;

    @ConstructorProperties({"guildId", "name", "iconId"})
    public GuildSettings(long guildId, @Nullable String name, @Nullable String iconId) {
        this.guildId = guildId;
        this.name = Optional.ofNullable(name);
        this.iconId = Optional.ofNullable(iconId);
    }

    public long getGuildId() {
        return this.guildId;
    }

    public String getName() {
        return this.name.orElse(DEFAULT_NAME);
    }

    public Optional<String> getIconId() {
        return this.iconId;
    }

    public Optional<String> getAvatarUrl() {
        return this.iconId
                .map(id -> "https://cdn.discordapp.com/icons/" + this.guildId + "/" + id + ".jpg");
    }

}
