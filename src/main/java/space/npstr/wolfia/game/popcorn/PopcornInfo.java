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

package space.npstr.wolfia.game.popcorn;

import net.dv8tion.jda.core.Permission;
import space.npstr.wolfia.game.CharakterSetup;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.game.definitions.Scope;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 20.06.17.
 * <p>
 * Static information about the popcorn game
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
    public GameMode getDefaultMode() {
        return GameMode.WILD;
    }

    @Override
    public Map<Permission, Scope> getRequiredPermissions(final GameMode mode) {
        final Map<Permission, Scope> requiredPermissions = new LinkedHashMap<>();
        requiredPermissions.put(Permission.MESSAGE_EMBED_LINKS, Scope.CHANNEL);
        requiredPermissions.put(Permission.MESSAGE_EXT_EMOJI, Scope.CHANNEL);
        switch (mode) {
            case CLASSIC:
                requiredPermissions.put(Permission.MESSAGE_MANAGE, Scope.CHANNEL); //prevent a bug where JDA will claim the bot has these permissions while it only has MANAGE_PERMISSIONS; request these first therefore and hope users give it both
                requiredPermissions.put(Permission.MANAGE_PERMISSIONS, Scope.CHANNEL);
                break;
            case WILD:
            default:
                break;
        }
        return requiredPermissions;
    }

    /**
     * @return a string representation of the allowed player count for this game and mode
     */
    @Override
    public String getAcceptablePlayerNumbers(final GameMode mode) {
        if (mode == GameMode.WILD) {
            return "3+";
        } else if (mode == GameMode.CLASSIC) {
            return "3 to 26";
        } else {
            throw new IllegalArgumentException("Unsupported game mode: " + mode.textRep);
        }
    }

    //the only reason Classic is capepd at 26 is the emoji limit to allow wolves giving a gun
    @Override
    public boolean isAcceptablePlayerCount(final int playerCount, final GameMode mode) {
        if (mode == GameMode.WILD) {
            return playerCount > 2;
        } else if (mode == GameMode.CLASSIC) {
            return 2 < playerCount && playerCount < 27;
        } else {
            throw new IllegalArgumentException("Unsupported game mode: " + mode.textRep);
        }

    }

    //https://weebs.are-la.me/e1613c.png
    //https://i.npstr.space/iyF.png
    //https://i.npstr.space/hsi.png
    @Nonnull
    @Override
    public CharakterSetup getCharacterSetup(@Nonnull final GameMode mode, final int playerCount) {

        if (!isAcceptablePlayerCount(playerCount, mode)) {
            throw new IllegalArgumentException(String.format(
                    "There is no possible character setup for the provided player count %s in this game %s mode %s",
                    playerCount, Games.POPCORN.textRep, mode.textRep)
            );
        }

        int wolves = (int) Math.ceil(playerCount / 3.0);
        //there is one special case for four players where this formula doesnt work out
        if (playerCount == 4) wolves = 1;
        final int villagers = playerCount - wolves;

        return new CharakterSetup()
                .addRoleAndAlignment(Alignments.VILLAGE, Roles.VANILLA, villagers)
                .addRoleAndAlignment(Alignments.WOLF, Roles.VANILLA, wolves);
    }

    @Override
    public String textRep() {
        return Games.POPCORN.textRep;
    }

    @Override
    public Games getGameType() {
        return Games.POPCORN;
    }
}
