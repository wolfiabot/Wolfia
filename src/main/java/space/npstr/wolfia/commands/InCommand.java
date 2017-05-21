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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.ICommand;
import space.npstr.wolfia.game.GameSetup;
import space.npstr.wolfia.game.Setups;

/**
 * Created by npstr on 23.08.2016
 */
public class InCommand implements ICommand {

    public static final String COMMAND = "in";
//    private final int MAX_SIGNUP_TIME = 10 * 60; //10h


    @Override
    public boolean argumentsValid(final String[] args, final MessageReceivedEvent event) {
//        if (args.length < 1)
//            return false;
//        try {
//            Long.valueOf(args[0]);
//        } catch (final NumberFormatException e) {
//            return false;
//        }
        return true;
    }

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {

//        long timeForSignup = Long.valueOf(args[0]);
//        timeForSignup = timeForSignup < this.MAX_SIGNUP_TIME ? timeForSignup : this.MAX_SIGNUP_TIME;

        final GameSetup setup = Setups.getAll().get(commandInfo.event.getChannel().getIdLong());
        if (setup == null) {
            Wolfia.handleOutputMessage(commandInfo.event.getChannel(),
                    "Please start setting up a game in with channel with `%s%s`", Config.PREFIX, SetupCommand.COMMAND);
            return;
        }

        setup.inPlayer(commandInfo.event.getAuthor().getIdLong());
        Wolfia.handleOutputMessage(commandInfo.event.getChannel(), setup.getStatus());
    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND + " <minutes>\nwill add you to the signup list for <minutes> "
                + "(up to 600 mins) and out you automatically afterwards or earlier if inactive```";
    }
}
