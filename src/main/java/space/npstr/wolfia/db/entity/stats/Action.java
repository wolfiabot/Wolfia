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

package space.npstr.wolfia.db.entity.stats;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by napster on 30.05.17.
 * <p>
 * Describe an action that happened during a game
 */
@Entity
@Table(name = "stats_action")
public class Action implements Serializable {

    private static final long serialVersionUID = -6803073458836067860L;

    //dont really care about this one, its for the database
    @Id
    @GeneratedValue
    @Column(name = "action_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @Column(name = "game")
    private Game game;

    //chronological order of the actions
    //just a failsafe in case the List of actions in Game gets into an unsorted state
    @Column(name = "order")
    private int order;

    // the difference between these two timestamps is the following: an action may be submitted before it actually
    // happens (example: nk gets submitted during the night, but actually "happens" when the day starts and results are
    // announced). these two timestamps try to capture that data as accurately as possible
    @Column(name = "time_stamp_submitted")
    private long timeStampSubmitted;

    @Column(name = "time_stamp_happened")
    private long timeStampHappened;

    //userId of the discord user; there might be special negative values for factional actors/targets in the future
    @Column(name = "actor")
    private long actor;

    //defined in the Actions enum
    @Column(name = "action_tape")
    private String actionType;

    //userId of the discord user
    @Column(name = "target")
    private long target;

    @Override

    public int hashCode() {
        final int prime = 31;
        //todo test if referencing game here produces any kind of errors, since it's marked as lazy loading
        int result = this.game.hashCode();
        result = prime * result + this.order;
        result = prime * result + (int) (this.timeStampSubmitted ^ (this.timeStampSubmitted >>> 32));
        result = prime * result + (int) (this.timeStampHappened ^ (this.timeStampHappened >>> 32));
        result = prime * result + (int) (this.actor ^ (this.actor >>> 32));
        result = prime * result + this.actionType.hashCode();
        result = prime * result + (int) (this.target ^ (this.target >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Action)) {
            return false;
        }
        final Action a = (Action) obj;
        //todo test if referencing game here produces any kind of errors, since it's marked as lazy loading
        return this.game.equals(a.game)
                && this.order == a.order
                && this.timeStampSubmitted == a.timeStampSubmitted
                && this.timeStampHappened == a.timeStampHappened
                && this.actor == a.actor
                && this.actionType.equals(a.actionType)
                && this.target == a.target;
    }

    //########## boilerplate code below

    Action() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Game getGame() {
        return this.game;
    }

    public void setGame(final Game game) {
        this.game = game;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public long getTimeStampSubmitted() {
        return this.timeStampSubmitted;
    }

    public void setTimeStampSubmitted(final long timeStampSubmitted) {
        this.timeStampSubmitted = timeStampSubmitted;
    }

    public long getTimeStampHappened() {
        return this.timeStampHappened;
    }

    public void setTimeStampHappened(final long timeStampHappened) {
        this.timeStampHappened = timeStampHappened;
    }

    public long getActor() {
        return this.actor;
    }

    public void setActor(final long actor) {
        this.actor = actor;
    }

    public String getActionType() {
        return this.actionType;
    }

    public void setActionType(final String actionType) {
        this.actionType = actionType;
    }

    public long getTarget() {
        return this.target;
    }

    public void setTarget(final long target) {
        this.target = target;
    }
}
