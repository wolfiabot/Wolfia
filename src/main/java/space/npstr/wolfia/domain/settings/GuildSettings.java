/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

import javax.annotation.Nullable;
import java.beans.ConstructorProperties;

public class GuildSettings {

    private final long guildId;
    private final String name;
    @Nullable
    private final String iconId;

    @ConstructorProperties({"guildId", "name", "iconId"})
    public GuildSettings(long guildId, String name, @Nullable String iconId) {
        this.guildId = guildId;
        this.name = name;
        this.iconId = iconId;
    }

    public long getGuildId() {
        return guildId;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getIconId() {
        return iconId;
    }

    public String getAvatarUrl() {
        return this.iconId == null ? null : "https://cdn.discordapp.com/icons/" + this.guildId + "/" + this.iconId + ".jpg";
    }

}
