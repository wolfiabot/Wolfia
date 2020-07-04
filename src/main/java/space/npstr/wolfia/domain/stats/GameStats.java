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

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import space.npstr.wolfia.game.definitions.Games;

import static space.npstr.wolfia.game.GameInfo.GameMode;

/**
 * Describe a game that happened
 */
public class GameStats {

    //this is pretty much an auto incremented id generator starting by 1 and going 1 upwards
    //there are no hard guarantees that there wont be any gaps, or that they will be in any order in the table
    //that's good enough for our use case though (giving games an "easy" to remember number to request replays and stats
    //later, and passively showing off how many games the bot has done)
    private Optional<Long> gameId = Optional.empty();

    private final Set<TeamStats> startingTeams = new HashSet<>();

    private final Set<ActionStats> actions = new HashSet<>();

    private final long startTime;

    private long endTime;

    private final long guildId;

    private final String guildName;

    private final long channelId;

    private final String channelName;

    private final Games gameType;

    private final GameMode gameMode;

    private final int playerSize;


    public GameStats(long guildId, String guildName, long channelId, String channelName, Games gameType,
                     GameMode gameMode, int playerSize) {

        this.guildId = guildId;
        this.guildName = guildName;
        this.channelId = channelId;
        this.channelName = channelName;
        this.startTime = System.currentTimeMillis();
        this.gameType = gameType;
        this.gameMode = gameMode;
        this.playerSize = playerSize;
    }

    // for jooq deserializing
    @ConstructorProperties({"gameId", "channelId", "channelName", "endTime", "gameMode", "gameType", "guildId",
            "guildName", "startTime", "playerSize"})
    public GameStats(long gameId, long channelId, String channelName, long endTime, String gameMode, String gameType,
                     long guildId, String guildName, long startTime, int playerSize) {

        this.gameId = Optional.of(gameId);
        this.channelId = channelId;
        this.channelName = channelName;
        this.endTime = endTime;
        this.gameMode = GameMode.valueOf(gameMode);
        this.gameType = Games.valueOf(gameType);
        this.guildId = guildId;
        this.guildName = guildName;
        this.startTime = startTime;
        this.playerSize = playerSize;
    }

    public void addAction(final ActionStats action) {
        this.actions.add(action);
    }

    public void addActions(final Collection<ActionStats> actions) {
        this.actions.addAll(actions);
    }

    public void setActions(final Collection<ActionStats> actions) {
        this.actions.clear();
        this.actions.addAll(actions);
    }

    public void addTeam(final TeamStats team) {
        this.startingTeams.add(team);
    }

    public void setTeams(final Collection<TeamStats> teams) {
        this.startingTeams.clear();
        this.startingTeams.addAll(teams);
    }

    //do not use the autogenerated id, it will only be set after persisting
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.guildId ^ (this.guildId >>> 32));
        result = prime * result + (int) (this.channelId ^ (this.channelId >>> 32));
        result = prime * result + (int) (this.startTime ^ (this.startTime >>> 32));
        return result;
    }

    //do not compare the autogenerated id, it will only be set after persisting
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof GameStats)) {
            return false;
        }
        final GameStats g = (GameStats) obj;
        return this.startTime == g.startTime && this.guildId == g.guildId && this.channelId == g.channelId;
    }

    public Optional<Long> getGameId() {
        return this.gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = Optional.of(gameId);
    }

    public Set<TeamStats> getStartingTeams() {
        return Collections.unmodifiableSet(this.startingTeams);
    }

    public Set<ActionStats> getActions() {
        return Collections.unmodifiableSet(this.actions);
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(final long endTime) {
        this.endTime = endTime;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public String getGuildName() {
        return this.guildName;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public Games getGameType() {
        return this.gameType;
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }

    public int getPlayerSize() {
        return this.playerSize;
    }

}
