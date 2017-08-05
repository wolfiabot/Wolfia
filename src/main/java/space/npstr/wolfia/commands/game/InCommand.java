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

import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.Banlist;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Scope;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by npstr on 23.08.2016
 */
public class InCommand extends BaseCommand {

    public static final String COMMAND = "in";
//    private final int MAX_SIGNUP_TIME = 10 * 60; //10h

    @Override
    public String help() {
        return Config.PREFIX + COMMAND +
                "\n#Add you to the signup list for this channel. You will play in the next starting game.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {

//        long timeForSignup = Long.valueOf(args[0]);
//        timeForSignup = timeForSignup < this.MAX_SIGNUP_TIME ? timeForSignup : this.MAX_SIGNUP_TIME;

        //is there a game going on?
        if (Games.get(commandInfo.event.getTextChannel().getIdLong()) != null) {
            Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(),
                    "%s, the game has already started! Please wait until it is over to join.",
                    TextchatUtils.userAsMention(commandInfo.event.getAuthor().getIdLong()));
            return false;
        }

        final SetupEntity setup = DbWrapper.getOrCreateEntity(commandInfo.event.getChannel().getIdLong(), SetupEntity.class);


        //force inn by bot owner
        if (commandInfo.event.getMessage().getMentionedUsers().size() > 0 && App.isOwner(commandInfo.event.getAuthor())) {
            commandInfo.event.getMessage().getMentionedUsers().forEach(u -> setup.inUser(u.getIdLong()));
            setup.postStatus();
            return true;
        }

        if (DbWrapper.getOrCreateEntity(commandInfo.event.getAuthor().getIdLong(), Banlist.class).getScope() == Scope.GLOBAL) {
            Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(),
                    "%s, lol ur banned.",
                    TextchatUtils.userAsMention(commandInfo.event.getAuthor().getIdLong()));
            return false;
        }

        if (setup.inUser(commandInfo.event.getAuthor().getIdLong())) {
            setup.postStatus();
            return true;
        } else {
            return false;
        }
    }
}
