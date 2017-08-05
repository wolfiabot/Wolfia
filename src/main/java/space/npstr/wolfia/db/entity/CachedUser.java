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

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.hibernate.annotations.ColumnDefault;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.IEntity;
import space.npstr.wolfia.db.PostgresHStoreConverter;

import javax.annotation.CheckReturnValue;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by napster on 29.07.17.
 * <p>
 * Caches some user values like their nicks
 */
@Entity
@Table(name = "cached_users")
public class CachedUser implements IEntity {

    @Id
    @Column(name = "user_id")
    private long userId;

    @Column(name = "name")
    private String name = "Uncached Username";

    @Column(name = "avatar_url")
    @ColumnDefault(value = "'http://i.imgur.com/Jm9SIGh.png'") //todo remove
    private String avatarUrl = "http://i.imgur.com/Jm9SIGh.png";

    @Column(name = "nicks", columnDefinition = "hstore")
    @Convert(converter = PostgresHStoreConverter.class)
    private final Map<String, String> nicks = new HashMap<>();

    //for JPA and IEntity
    public CachedUser() {
    }

    public static CachedUser get(final long userId) {
        return DbWrapper.getOrCreateEntity(userId, CachedUser.class);
    }

    public CachedUser save() {
        return DbWrapper.merge(this);
    }

    @CheckReturnValue
    public CachedUser set(final Member member) {
        if (member == null) {
            return this; //gracefully ignore null members
        }
        final CachedUser cu = this.set(member.getUser());

        final String nick = member.getNickname();
        if (nick != null)
            return cu.setNick(member.getGuild().getIdLong(), member.getNickname());

        return cu;
    }

    @CheckReturnValue
    public CachedUser set(final User user) {
        if (user == null) {
            return this; //gracefully ignore null users
        }
        return this.setName(user.getName())
                .setAvatarUrl(user.getEffectiveAvatarUrl());
    }

    @Override
    @CheckReturnValue
    public void setId(final long id) {
        this.userId = id;
    }

    @Override
    public long getId() {
        return this.userId;
    }

    public String getName() {
        return this.name;
    }

    @CheckReturnValue
    public CachedUser setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the name of this user in the provided guild (nick or if no nick set the user name), similar to JDA's getEffectiveName()
     */
    public String getNick(final long guildId) {
        final String nick = this.nicks.get(Long.toString(guildId));
        return nick != null ? nick : getName();
    }

    @CheckReturnValue
    public CachedUser setNick(final long guildId, final String nick) {
        this.nicks.put(Long.toString(guildId), nick);
        return this;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    @CheckReturnValue
    public CachedUser setAvatarUrl(final String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }
}
