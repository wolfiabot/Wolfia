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
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by napster on 25.07.17.
 * <p>
 * Represents a discord guild; just dump data in here
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

    public EGuild() {
    }

    //calling this from eval todo remove
    public static void migrate() {
        for (final Guild g : Wolfia.jda.getGuilds()) {
            DbWrapper.getEntity(g.getIdLong(), EGuild.class).join();
        }
    }

    public void join() {
        this.joined = System.currentTimeMillis();
        this.present = true;
        DbWrapper.merge(this);
    }

    public void leave() {
        this.left = System.currentTimeMillis();
        this.present = false;
        DbWrapper.merge(this);
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

    // getters setters boilerplate below

    @Override
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

    public void setJoined(final long joined) {
        this.joined = joined;
    }

    public long getLeft() {
        return this.left;
    }

    public void setLeft(final long left) {
        this.left = left;
    }

    public boolean isPresent() {
        return this.present;
    }

    public void setPresent(final boolean present) {
        this.present = present;
    }
}
