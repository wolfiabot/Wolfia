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

import space.npstr.wolfia.db.IEntity;
import space.npstr.wolfia.game.definitions.Scope;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by napster on 07.07.17.
 */
@Entity
@Table(name = "banlist")
public class Banlist implements IEntity {

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


    @Override
    public void setId(final long id) {
        this.userId = id;
    }

    @Override
    public long getId() {
        return this.userId;
    }

    public Scope getScope() {
        return Scope.valueOf(this.scope);
    }

    public void setScope(final Scope scope) {
        this.scope = scope.name();
    }
}
