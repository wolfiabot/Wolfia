/*
 * Copyright (C) 2016-2020 the original author or authors
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

import java.util.Optional;
import org.springframework.lang.Nullable;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Roles;

/**
 * Describe a participant of a game
 */
public class PlayerStats {

    private Optional<Long> playerId = Optional.empty();

    private final TeamStats team;

    private final long userId;

    @Nullable
    private final String nickname;

    private final Alignments alignment;

    private final Roles role;

    private int totalPosts;

    private int totalPostLength;

    public PlayerStats(TeamStats team, long userId, @Nullable String nick, Alignments alignment, Roles role) {
        this.team = team;
        this.userId = userId;
        this.nickname = nick;
        this.alignment = alignment;
        this.role = role;
        this.totalPosts = 0;
        this.totalPostLength = 0;
    }

    // for use in the stats repository
    public PlayerStats(long playerId, @Nullable String nickname, String role, int totalPostLength, int totalPosts, long userId,
                       TeamStats teamStats, String alignment) {

        this.playerId = Optional.of(playerId);
        this.nickname = nickname;
        this.role = Roles.valueOf(role);
        this.totalPostLength = totalPostLength;
        this.totalPosts = totalPosts;
        this.userId = userId;
        this.team = teamStats;
        this.alignment = Alignments.valueOf(alignment);
    }

    public synchronized void bumpPosts(final int length) {
        this.totalPosts++;
        this.totalPostLength += length;
    }

    //do not use the autogenerated id, it will only be set after persisting
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = this.team.hashCode();
        result = prime * result + (int) (this.userId ^ (this.userId >>> 32));
        return result;
    }

    //do not compare the autogenerated id, it will only be set after persisting
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof PlayerStats)) {
            return false;
        }
        final PlayerStats p = (PlayerStats) obj;
        return this.userId == p.userId && this.team.equals(p.team);
    }

    //########## boilerplate code below

    public Optional<Long> getPlayerId() {
        return this.playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = Optional.of(playerId);
    }

    public TeamStats getTeam() {
        return team;
    }

    public long getUserId() {
        return this.userId;
    }

    @Nullable
    public String getNickname() {
        return this.nickname;
    }

    public Alignments getAlignment() {
        return this.alignment;
    }

    public Roles getRole() {
        return this.role;
    }

    public synchronized int getTotalPosts() {
        return this.totalPosts;
    }

    public synchronized int getTotalPostLength() {
        return this.totalPostLength;
    }
}
