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

package space.npstr.wolfia.commands.game;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.commands.IGameCommand;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.TextchatUtils;

/**
 * Created by napster on 21.05.17.
 */
public class ShootCommand implements ICommand, IGameCommand {


    public static final String COMMAND = "shoot";

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {

        if (commandInfo.event.getMessage().getMentionedUsers().size() < 1) {
            Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(), "%s", help());
            return;
        }

        final Game game = Games.get(commandInfo.event.getChannel().getIdLong());
        if (game == null) {
            Wolfia.handleOutputMessage(commandInfo.event.getChannel(),
                    "Hey %s, there is no game currently going on in here.",
                    TextchatUtils.userAsMention(commandInfo.event.getAuthor().getIdLong()));
            return;
        }

        try {
            game.issueCommand(this, commandInfo);
        } catch (final IllegalGameStateException e) {
            Wolfia.handleOutputMessage(commandInfo.event.getChannel(), "%s", e.getMessage());
        }

    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND + " @user\nshoot someone```";
    }
}
