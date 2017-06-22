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

import net.dv8tion.jda.core.Permission;
import space.npstr.wolfia.game.definitions.Scope;

import java.util.*;

/**
 * Created by napster on 20.06.17.
 */
public class PopcornInfo implements GameInfo {

    @Override
    public List<GameMode> getSupportedModes() {
        final List<GameMode> supportedModes = new ArrayList<>();
        supportedModes.add(GameMode.WILD);
        supportedModes.add(GameMode.CLASSIC);
        return supportedModes;
    }

    @Override
    public GameMode getDefaultgMode() {
        return GameMode.WILD;
    }

    @Override
    public Map<Scope, Permission> getRequiredPermissions(final GameMode mode) {
        final Map<Scope, Permission> requiredPermissions = new TreeMap<>();
        switch (mode) {
            case CLASSIC:
                requiredPermissions.put(Scope.CHANNEL, Permission.MANAGE_PERMISSIONS);
            case WILD:
            default:
                requiredPermissions.put(Scope.CHANNEL, Permission.MESSAGE_EMBED_LINKS);
                requiredPermissions.put(Scope.CHANNEL, Permission.MESSAGE_EXT_EMOJI);
        }
        return requiredPermissions;
    }

    /**
     * @return player numbers that this game supports
     */
    @Override
    public Set<Integer> getAcceptablePlayerNumbers(final GameMode mode) {
        final Set<Integer> foo = new HashSet<>();
        //3ers for debugging and fucking around I guess
        foo.add(3); //1 wolf, 2 town
        foo.add(6); //2 wolf, 4 town
        foo.add(8); //3 wolf, 5 town
        foo.add(9); //3 wolf, 6 town
        foo.add(10); //4 wolf, 6 town
        foo.add(11); // the regular game 4 wolf 7 town
        return foo;
    }
}
