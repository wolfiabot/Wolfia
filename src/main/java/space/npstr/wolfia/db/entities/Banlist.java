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

import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.wolfia.game.definitions.Scope;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by napster on 07.07.17.
 */
@Entity
@Table(name = "banlist")
public class Banlist extends SaucedEntity<Long, Banlist> {

    //user ids banned from playing
    @Id
    @Column(name = "user_id")
    private long userId;

    @Column(name = "scope")
    private String scope = Scope.NONE.name();

    //for JPA and IEntity
    public Banlist() {
    }

    public Banlist(final long id, final Scope scope) {
        this.userId = id;
        this.scope = scope.name();
    }


    @Nonnull
    @Override
    public Banlist setId(final Long id) {
        this.userId = id;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
        return this.userId;
    }

    public Scope getScope() {
        return Scope.valueOf(this.scope);
    }

    @Nonnull
    public Banlist setScope(@Nonnull final Scope scope) {
        this.scope = scope.name();
        return this;
    }

    @Nonnull
    public static Banlist load(final long userId) throws DatabaseException {
        return SaucedEntity.load(EntityKey.of(userId, Banlist.class));
    }
}
