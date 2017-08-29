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

package space.npstr.wolfia.commands.ingame;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by napster on 06.08.17.
 */
public class NightkillCommand extends GameCommand {

    public NightkillCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        //the nightkill command will always be called from a private guild, and only one game is allowed to run in
        //a private guild at the time
        Game game = null;
        for (final Game g : Games.getAll().values()) {
            if (g.getPrivateGuildId() == commandInfo.event.getGuild().getIdLong()) {
                game = g;
                break;
            }
        }

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

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger() + " name or number"
                + "\n#Vote a player for nightkill. Make sure to use the player's number if the names are ambiguous";
    }
}
