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

package space.npstr.wolfia.db.entity;

import net.dv8tion.jda.core.entities.Guild;
import org.hibernate.annotations.ColumnDefault;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.IEntity;

import javax.annotation.CheckReturnValue;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by napster on 25.07.17.
 * <p>
 * Represents a discord guild
 * Caches some stuff similar to CachedUser
 * Just dump data in here
 */
@Entity
@Table(name = "guilds")
public class EGuild implements IEntity {

    @Id
    @Column(name = "guild_id", nullable = false)
    private long guildId;

    //when did we join this
    @Column(name = "joined_timestamp")
    private long joined = -1;

    //when did we leave this
    @Column(name = "left_timestamp")
    private long left = -1;

    //are we currently in there or not?
    @Column(name = "present")
    private boolean present = false;

    @Column(name = "avatar_url")
    @ColumnDefault(value = "'http://i.imgur.com/Jm9SIGh.png'") //todo remove
    private String avatarUrl = "http://i.imgur.com/Jm9SIGh.png";

    @Column(name = "name")
    @ColumnDefault(value = "'Uncached Guild'")//todo remove
    private String name = "Uncached Guild";

    //for jpa / IEntity
    public EGuild() {
    }

    public static EGuild get(final long guildId) {
        return DbWrapper.getOrCreateEntity(guildId, EGuild.class);
    }

    public EGuild save() {
        return DbWrapper.merge(this);
    }

    public EGuild set(final Guild guild) {
        if (guild == null) {
            return this;//gracefully ignore null guilds
        }
        return this.setName(guild.getName())
                .setAvatarUrl(guild.getIconUrl());
    }


    @CheckReturnValue
    public EGuild join() {
        this.joined = System.currentTimeMillis();
        this.present = true;
        return this;
    }

    @CheckReturnValue
    public EGuild leave() {
        this.left = System.currentTimeMillis();
        this.present = false;
        return this;
    }


    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof EGuild)) {
            return false;
        }
        final EGuild other = (EGuild) obj;
        return other.guildId == this.guildId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.guildId);
    }

    @Override
    @CheckReturnValue
    public void setId(final long id) {
        this.guildId = id;
    }

    @Override
    public long getId() {
        return this.guildId;
    }

    public long getJoined() {
        return this.joined;
    }

    public long getLeft() {
        return this.left;
    }

    public boolean isPresent() {
        return this.present;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    @CheckReturnValue
    public EGuild setAvatarUrl(final String avatarUrl) {
        if (avatarUrl == null || "".equals(avatarUrl)) {
            this.avatarUrl = "http://i.imgur.com/Jm9SIGh.png";
        } else {
            this.avatarUrl = avatarUrl;
        }
        return this;
    }

    public String getName() {
        return this.name;
    }

    @CheckReturnValue
    public EGuild setName(final String name) {
        this.name = name;
        return this;
    }
}
