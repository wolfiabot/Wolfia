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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import space.npstr.wolfia.game.definitions.Alignments;

public class InsertTeamStats {

    private final Set<InsertPlayerStats> players = new HashSet<>();
    private final Alignments alignment;
    // teams of the same alignment (example: wolves) should have unique names
    private final String name;
    private boolean isWinner = false;
    private int teamSize;


    public InsertTeamStats(Alignments alignment, String name, int teamSize) {
        this.alignment = alignment;
        this.name = name;
        this.teamSize = teamSize;
    }

    public void addPlayer(InsertPlayerStats player) {
        this.players.add(player);
    }

    public void setPlayers(Collection<InsertPlayerStats> players) {
        this.players.clear();
        this.players.addAll(players);
    }


    @Override
    public int hashCode() {
        int prime = 31;
        int result = this.alignment.hashCode();
        result = prime * result + this.name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InsertTeamStats t)) {
            return false;
        }
        return this.alignment.equals(t.alignment) && this.name.equals(t.name);
    }

    public Set<InsertPlayerStats> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    public Alignments getAlignment() {
        return this.alignment;
    }

    public String getName() {
        return this.name;
    }

    public boolean isWinner() {
        return this.isWinner;
    }

    public void setWinner(boolean winner) {
        this.isWinner = winner;
    }

    public int getTeamSize() {
        return this.teamSize;
    }

    public void setTeamSize(int teamSize) {
        this.teamSize = teamSize;
    }
}
