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

import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.StatusCommand;
import space.npstr.wolfia.utils.TextchatUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Builds a game in a channel
 * Keeps track of sign ups and starts the game when everything is set up
 */
public class GameSetup {

    //internal values
    private final long channelId;

    //setup values with defaults
    private final Game game;
    private final Set<Long> innedPlayers = new HashSet<>();

    public GameSetup(final long channelId) {
        this.channelId = channelId;
        this.game = new Popcorn(channelId);
    }

    public long getChannelId() {
        return this.channelId;
    }

    public void inPlayer(final long userId) {
        if (this.innedPlayers.contains(userId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s you have inned already.", TextchatUtils.userAsMention(userId));
        } else {
            //TODO any checks preventing a player from inning? missing permissions, too many permissions breaking the game, bans, whatever?
            this.innedPlayers.add(userId);
        }
    }

    public void outPlayer(final long userId) {
        this.innedPlayers.remove(userId);
    }

    public void startGame() {

        if (!this.game.isAcceptablePlayerCount(this.innedPlayers.size())) {
            Wolfia.handleOutputMessage(this.channelId,
                    "There aren't enough (or too many) players signed up! Please use `%s%s` for more information",
                    Config.PREFIX, StatusCommand.COMMAND);
        }

        Setups.remove(this);
        Games.set(this.game);
        this.game.start(this.innedPlayers);
    }

    public String getStatus() {
        final StringBuilder sb = new StringBuilder(this.game.getStatus()).append("\n");
        this.innedPlayers.forEach(userId -> sb.append(TextchatUtils.userAsMention(userId)).append(" "));
        return sb.toString();
    }
}
