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
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.game.Setup;
import space.npstr.wolfia.game.Setups;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.TextchatUtils;

/**
 * Created by npstr on 23.08.2016
 */
public class InCommand implements ICommand {

    public static final String COMMAND = "in";
//    private final int MAX_SIGNUP_TIME = 10 * 60; //10h

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {

//        long timeForSignup = Long.valueOf(args[0]);
//        timeForSignup = timeForSignup < this.MAX_SIGNUP_TIME ? timeForSignup : this.MAX_SIGNUP_TIME;

        //is there a game going on?
        if (Games.get(commandInfo.event.getTextChannel().getIdLong()) != null) {
            Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(),
                    "%s, the game has already started! Please wait until it is over to join.",
                    TextchatUtils.userAsMention(commandInfo.event.getAuthor().getIdLong()));
            return;
        }
        Setup setup = Setups.getAll().get(commandInfo.event.getChannel().getIdLong());
        if (setup == null) {
            setup = Setups.createNew(commandInfo.event.getChannel().getIdLong());
        }
        final Setup s = setup;


        //force inn by bot owner
        if (commandInfo.event.getMessage().getMentionedUsers().size() > 0 && commandInfo.event.getAuthor().getIdLong() == App.OWNER_ID) {
            commandInfo.event.getMessage().getMentionedUsers().forEach(u -> s.inPlayer(u.getIdLong(),
                    () -> Wolfia.handleOutputMessage(commandInfo.event.getChannel(), "%s", s.getStatus())));
            return;
        }

        s.inPlayer(commandInfo.event.getAuthor().getIdLong(), () -> Wolfia.handleOutputMessage(commandInfo.event.getChannel(), "%s", s.getStatus()));
    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND + " <minutes>\nwill add you to the signup list for <minutes> "
                + "(up to 600 mins) and out you automatically afterwards or earlier if inactive```";
    }
}
