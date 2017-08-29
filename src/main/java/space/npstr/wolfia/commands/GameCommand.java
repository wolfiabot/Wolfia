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

package space.npstr.wolfia.commands;

import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by napster on 21.05.17.
 * <p>
 * Game commands are different from regular commands as they can be registered by games.
 * todo some game commands need to work in private channels
 * todo find a decent way to go from private channel -> game that the command belongs to.
 * todo limiting users to only a single ongoing game is an option, but this happens 95% of the time for when there is a
 * todo broken game / game with bad settings and users just want to start a new one, so stopping them from doing that
 * todo would be a frustrating experience
 */
public abstract class GameCommand extends BaseCommand {

    public GameCommand(final String trigger, final String... triggers) {
        super(trigger, triggers);
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {

        final Game game = Games.get(commandInfo.event.getChannel().getIdLong());
        if (game == null) {
            Wolfia.handleOutputMessage(commandInfo.event.getChannel(),
                    "Hey %s, there is no game currently going on in here.",
                    TextchatUtils.userAsMention(commandInfo.event.getAuthor().getIdLong()));
            return false;
        }

        try {
            return game.issueCommand(this, commandInfo);
        } catch (final IllegalGameStateException e) {
            Wolfia.handleOutputMessage(commandInfo.event.getChannel(), "%s", e.getMessage());
            return false;
        }
    }

    /**
     * @return whether the provided string is a command trigger or not (like "shoot" for the shoot command for example)
     */
    public boolean isCommandTrigger(final String command) {
        throw new UnsupportedOperationException("isCommandTrigger not implemented for this game command");
    }
}
