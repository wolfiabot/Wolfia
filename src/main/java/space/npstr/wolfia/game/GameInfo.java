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

package space.npstr.wolfia.game;

import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.Permission;
import space.npstr.wolfia.game.definitions.Scope;

/**
 * Should provide some static information about a game.
 */
public interface GameInfo {

    //WILD and CLASSIC are used by Popcorn
    //PURE and LITE are used by Mafia
    //XMAS is a seasonal mode
    enum GameMode {
        WILD("Wild"),
        CLASSIC("Classic"),
        PURE("Pure"),
        LITE("Lite"),
        XMAS("Xmas"),
        //
        ;

        public final String textRep;

        GameMode(String textRep) {
            this.textRep = textRep;
        }


        @Override
        public String toString() {
            return this.textRep;
        }
    }

    List<GameMode> getSupportedModes();

    GameMode getDefaultMode();

    /**
     * Smaller permissions need to be at the top of the returned Map to prevent bugs where JDA _thinks_ we have a
     * permission to do something because we have a higher hierarchical permission but Discord disagrees.
     */
    Map<Permission, Scope> getRequiredPermissions(GameMode mode);

    String getAcceptablePlayerNumbers(GameMode mode);

    boolean isAcceptablePlayerCount(int playerCount, GameMode mode);

    CharakterSetup getCharacterSetup(GameMode mode, int playerCount);

    String textRep();
}
