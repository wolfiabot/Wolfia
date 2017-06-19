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

package space.npstr.wolfia.game;

import net.dv8tion.jda.core.entities.Message;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IGameCommand;
import space.npstr.wolfia.utils.IllegalGameStateException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by npstr on 14.09.2016
 * <p>
 * creating these should be lightweight and not cause any permanent "damage"
 * they may be discarded a few times before one actually starts
 * a created game that hasn't started can answer questions about the modes it supports, supported player counts, etc
 * <p>
 * on contrast, starting a game is serious business
 * it needs to receive a unique number (preferably increasing)
 * it will have to cause outputs in the main game channel and in private channels for role pms
 * all this means a started game has to be treated carefully, both for data consistency and to keep salt levels due to
 * technical problems at bay
 */
public abstract class Game {

    //hideous construction...this is what happens when you can't override static methods todo seriously, find a better way
    protected static final Map<Games, Set<String>> MODES_REGISTRY = new HashMap<>();
    protected static final Map<Games, Set<Integer>> ACCEPTABLE_PLAYER_NUMBERS_REGISTRY = new HashMap<>();

    static {
        MODES_REGISTRY.put(Games.POPCORN, Arrays.stream(Popcorn.MODE.values()).map(Enum::name).collect(Collectors.toSet()));

        final Set<Integer> foo = new HashSet<>();
        //for debugging and fucking around I guess
        foo.add(3); //1 wolf, 2 town
        foo.add(6); //2 wolf, 4 town
        foo.add(8); //3 wolf, 5 town
        foo.add(9); //3 wolf, 6 town
        foo.add(10); //4 wolf, 6 town
        foo.add(11); // the regular game 4 wolf 7 town
        ACCEPTABLE_PLAYER_NUMBERS_REGISTRY.put(Games.POPCORN, Collections.unmodifiableSet(foo));
    }

    public static Set<String> getGameModes(final Games g) {
        return MODES_REGISTRY.get(g);
    }

    public static Set<Integer> getAcceptablePlayerNumbers(final Games g) {
        return ACCEPTABLE_PLAYER_NUMBERS_REGISTRY.get(g);
    }

    public abstract Set<Integer> getAcceptedPlayerNumbers();

    public abstract boolean start(Set<Long> players);

    public abstract boolean isAcceptablePlayerCount(int signedUp);

    public abstract void setMode(String mode) throws IllegalGameStateException;

    public abstract void setDayLength(long millis);

    public abstract List<String> getPossibleModes();

    public abstract void issueCommand(IGameCommand command, CommandParser.CommandContainer commandInfo) throws IllegalGameStateException;

    /**
     * this should revert each and everything the game touches in terms of discord roles and permissions to normal
     * most likely this includes deleting all discord roles used in the game and resetting @everyone permissions for the game channel
     */
    public abstract void resetRolesAndPermissions();

    /**
     * @return Returns the main channel where the game is running
     */
    public abstract long getChannelId();

    public abstract void setChannelId(long channelId);


    /**
     * @return a status of the game
     */
    public abstract String getStatus();

    /**
     * @return the role pm of a user
     */
    public abstract String getRolePm(long userId);

    /**
     * @return true if the user is playing in this game (dead or alive), false if not
     */
    public abstract boolean isUserPlaying(long userId);


    /**
     * this is used to keep stats
     */
    public abstract void userPosted(Message message);

    /**
     * completely clean up a running game
     * aka reset any possible permissions and overrides, stop any running threads etc
     */
    public abstract void cleanUp();
}
