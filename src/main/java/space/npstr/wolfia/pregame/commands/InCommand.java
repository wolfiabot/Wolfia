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

package space.npstr.wolfia.pregame.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.Command;
import space.npstr.wolfia.CommandListener;
import space.npstr.wolfia.Main;
import space.npstr.wolfia.pregame.Pregame;
import space.npstr.wolfia.utils.Player;

/**
 * Created by npstr on 23.08.2016
 */
public class InCommand extends Command {

    public static final String COMMAND = "in";
    private final String HELP = "```usage: " + getListener().getPrefix() + COMMAND + " <minutes>\nwill add you to the" +
            " signup list for <minutes> (up to 600 mins) and out you automatically afterwards or earlier if inactive```";
    private final int MAX_SIGNUP_TIME = 10 * 60; //10h

    private final Pregame pg;

    public InCommand(CommandListener l, Pregame pg) {
        super(l);
        this.pg = pg;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        if (args.length < 1)
            return false;
        try {
            Long.valueOf(args[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        //check SING UP counter
        if (Player.getSingups(event.getAuthor().getId()) >= 10) {
            Main.handleOutputMessage(event.getTextChannel(), "Hold on champ. You have reached 100 SING UPs. Submit a " +
                    "karaoke video featuring yourself and ask an admin to reset your SING UP counter.");
            return false;
        }

        long timeForSignup = Long.valueOf(args[0]);
        timeForSignup = timeForSignup < MAX_SIGNUP_TIME ? timeForSignup : MAX_SIGNUP_TIME;
        pg.inPlayer(event.getAuthor().getId(), timeForSignup);

        return true;
    }

    @Override
    public String help() {
        return HELP;
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent event) {
        if (!success) {
            Main.handleOutputMessage(event.getTextChannel(), Player.asMention(event.getAuthor().getId()) + ":\n" + help());
        }
    }
}
