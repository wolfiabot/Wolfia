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

package space.npstr.wolfia.db.type;

import java.util.Optional;

/**
 * Discord OAuth2 scopes supported by us.
 * See https://discord.com/developers/docs/topics/oauth2#shared-resources-oauth2-scopes for the full list.
 */
public enum OAuth2Scope {

    IDENTIFY("identify"),
    GUILDS("guilds"),
    GUILD_JOIN("guilds.join");

    private final String discordName;

    OAuth2Scope(String discordName) {
        this.discordName = discordName;
    }

    public String discordName() {
        return this.discordName;
    }

    public static Optional<OAuth2Scope> parse(String input) {
        for (OAuth2Scope scope : OAuth2Scope.values()) {
            if (scope.discordName().equalsIgnoreCase(input)) {
                return Optional.of(scope);
            }
            if (scope.name().equalsIgnoreCase(input)) {
                return Optional.of(scope);
            }
        }

        return Optional.empty();
    }
}
