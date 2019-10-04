/*
 * Copyright (C) 2017 Dennis Neufeld
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

package space.npstr.wolfia.db.entities;

import space.npstr.sqlsauce.entities.discord.DiscordUser;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.wolfia.Launcher;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by napster on 29.07.17.
 * <p>
 * Caches some user values like their nicks
 */
@Entity
@Table(name = "cached_user")
public class CachedUser extends DiscordUser<CachedUser> {


    public CachedUser() {
        //for JPA and IEntity
    }

    public static CachedUser load(final long userId) {
        return Launcher.getBotContext().getDatabase().getWrapper().getOrCreate(EntityKey.of(userId, CachedUser.class));
    }
}
