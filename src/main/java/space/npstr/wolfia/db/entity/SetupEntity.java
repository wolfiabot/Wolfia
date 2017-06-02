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

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by napster on 29.05.17.
 * <p>
 * database representation of a Setup object
 */
@Entity
@Table(name = "setups")
public class SetupEntity implements IEntity {

    @Id
    @Column(name = "channel_id")
    private long channelId = -1;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "inned_players")
    private Set<Long> innedPlayers = new HashSet<>();

    //one of the values of the Games.GAMES enum
    @Column(name = "game")
    private String game = "";

    //optional mode, for example CLASSIC or WILD for Popcorn games
    @Column(name = "mode")
    private String mode = "";


    public SetupEntity(final long channelId, final Set<Long> innedPlayers, final String game) {
        this.channelId = channelId;
        this.innedPlayers = innedPlayers;
        this.game = game;
    }

    @Override
    public void setId(final long id) {
        this.channelId = id;
    }

    @Override
    public long getId() {
        return this.channelId;
    }


    //below is boilerplate code for hibernate/jpa
    public SetupEntity() {
    }

    public long getChannelId() {
        return this.channelId;
    }

    public void setChannelId(final long channelId) {
        this.channelId = channelId;
    }

    public Set<Long> getInnedPlayers() {
        return this.innedPlayers;
    }

    public void setInnedPlayers(final Set<Long> innedPlayers) {
        this.innedPlayers = innedPlayers;
    }

    public String getGame() {
        return this.game;
    }

    public void setGame(final String game) {
        this.game = game;
    }

    public String getMode() {
        return this.mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }
}
