/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.domain.stats;

import org.springframework.lang.Nullable;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Phase;

/**
 * Describe an action that happened during a game
 */
public class InsertActionStats {

    //chronological order of the actions
    //order is a reserved keyword in postgres, so we use 'sequence' in the table instead
    private final int order;
    // the difference between these two timestamps is the following: an action may be submitted before it actually
    // happens (example: nk gets submitted during the night, but actually "happens" when the day starts and results are
    // announced). these two timestamps try to capture that data as accurately as possible
    private final long timeStampSubmitted;
    private long timeStampHappened;
    //n0, d1 + n1, d2 + n2 etc
    private final int cycle;
    private final Phase phase;
    //userId of the discord user; there might be special negative values for factional actors/targets in the future
    private final long actor;
    private final Actions actionType;
    //userId of the discord user
    private final long target;
    //save any additional info of an action in here
    @Nullable
    private String additionalInfo;

    public InsertActionStats(int order, long timeStampSubmitted, long timeStampHappened, int cycle,
                             Phase phase, long actor, Actions action, long target, @Nullable String additionalInfo) {

        this.order = order;
        this.timeStampSubmitted = timeStampSubmitted;
        this.timeStampHappened = timeStampHappened;
        this.cycle = cycle;
        this.phase = phase;
        this.actor = actor;
        this.actionType = action;
        this.target = target;
        this.additionalInfo = additionalInfo;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = this.order;
        result = prime * result + (int) (this.timeStampSubmitted ^ (this.timeStampSubmitted >>> 32));
        result = prime * result + (int) (this.timeStampHappened ^ (this.timeStampHappened >>> 32));
        result = prime * result + (int) (this.actor ^ (this.actor >>> 32));
        result = prime * result + this.actionType.hashCode();
        result = prime * result + (int) (this.target ^ (this.target >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InsertActionStats a)) {
            return false;
        }
        return this.order == a.order
                && this.timeStampSubmitted == a.timeStampSubmitted
                && this.timeStampHappened == a.timeStampHappened
                && this.actor == a.actor
                && this.actionType.equals(a.actionType)
                && this.target == a.target;
    }

    public int getOrder() {
        return this.order;
    }

    public long getTimeStampSubmitted() {
        return this.timeStampSubmitted;
    }

    public long getTimeStampHappened() {
        return this.timeStampHappened;
    }

    public void setTimeStampHappened(long timeStampHappened) {
        this.timeStampHappened = timeStampHappened;
    }

    public int getCycle() {
        return this.cycle;
    }

    public Phase getPhase() {
        return this.phase;
    }

    public long getActor() {
        return this.actor;
    }

    public Actions getActionType() {
        return this.actionType;
    }

    public long getTarget() {
        return this.target;
    }

    @Nullable
    public String getAdditionalInfo() {
        return this.additionalInfo;
    }

    /**
     * @return itself for chaining
     */
    public InsertActionStats setAdditionalInfo(@Nullable String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }
}
