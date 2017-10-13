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

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlstack.DatabaseException;
import space.npstr.sqlstack.DatabaseWrapper;
import space.npstr.sqlstack.converters.PostgresHStoreConverter;
import space.npstr.sqlstack.entities.SaucedEntity;
import space.npstr.wolfia.Wolfia;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
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
public class CachedUser extends SaucedEntity<Long, CachedUser> {

    @Id
    @Column(name = "user_id")
    private long userId;

    @Column(name = "name")
    private String name = "Uncached Username";

    @Column(name = "avatar_url")
    private String avatarUrl = "http://i.imgur.com/Jm9SIGh.png";

    @Column(name = "nicks", columnDefinition = "hstore")
    @Convert(converter = PostgresHStoreConverter.class)
    private final Map<String, String> nicks = new HashMap<>();

    //for JPA and IEntity
    public CachedUser() {
    }

    //always returns the cached object, resulting in a call to the database; for faster lookups consider using hybrid
    //methods below that try to look up a live user/member first
    public static CachedUser load(final DatabaseWrapper dbWrapper, final long userId) throws DatabaseException {
        return dbWrapper.getOrCreate(userId, CachedUser.class);
    }

    public static String getName(final DatabaseWrapper dbWrapper, final long userId) throws DatabaseException {
        final User user = Wolfia.getUserById(userId);
        if (user != null) {
            return user.getName();
        } else {
            return CachedUser.load(dbWrapper, userId).name;
        }
    }

    public static String getNick(final DatabaseWrapper dbWrapper, final long userId, final long guildId) throws DatabaseException {
        final Guild guild = Wolfia.getGuildById(guildId);
        if (guild != null) {
            final Member member = guild.getMemberById(userId);
            if (member != null) return member.getEffectiveName();
        }

        return CachedUser.load(dbWrapper, userId).getNick(guildId);
    }

    public static String getAvatarUrl(final DatabaseWrapper dbWrapper, final long userId) throws DatabaseException {
        final User user = Wolfia.getUserById(userId);
        if (user != null) {
            return user.getAvatarUrl();
        } else {
            return CachedUser.load(dbWrapper, userId).avatarUrl;
        }
    }

    //call this when you only really want to write data
    public static CachedUser cache(final DatabaseWrapper dbWrapper, final Member member) throws DatabaseException {
        return CachedUser.load(dbWrapper, member.getUser().getIdLong())
                .set(member)
                .save();
    }

    //call this when you only really want to write data
    public static CachedUser cache(final DatabaseWrapper dbWrapper, final User user) throws DatabaseException {
        return CachedUser.load(dbWrapper, user.getIdLong())
                .set(user)
                .save();
    }

    @CheckReturnValue
    public CachedUser set(final Member member) {
        if (member == null) {
            return this; //gracefully ignore null members
        }
        final CachedUser cu = this.set(member.getUser());

        final String nick = member.getNickname();
        if (nick != null) {
            return cu.setNick(member.getGuild().getIdLong(), nick);
        } else {
            return cu.removeNick(member.getGuild().getIdLong());
        }
    }

    @CheckReturnValue
    public CachedUser set(final User user) {
        if (user == null) {
            return this; //gracefully ignore null users
        }
        return this.setName(user.getName())
                .setAvatarUrl(user.getEffectiveAvatarUrl());
    }

    @Nonnull
    @Override
    @CheckReturnValue
    public CachedUser setId(final Long id) {
        this.userId = id;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
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

    @CheckReturnValue
    public CachedUser removeNick(final long guildId) {
        this.nicks.remove(Long.toString(guildId));
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
