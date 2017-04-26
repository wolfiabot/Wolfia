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
import space.npstr.wolfia.utils.Player;

/**
 * Created by npstr on 06.09.2016
 */
public class SingUpCommand extends Command {

    public static final String COMMAND = "singups";

    public SingUpCommand(CommandListener listener) {
        super(listener);
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        int singups = Player.singup(event.getAuthor().getId());
        if (singups < 10) {
            Main.handleOutputMessage(event.getTextChannel(), event.getAuthor().getAsMention() + " called for SING UPs "
                    + singups + " times! He is required to submit a karaoke video at 10 SING UPs :)");
        } else {
            Main.handleOutputMessage(event.getTextChannel(), "Congratulations, " + event.getAuthor().getAsMention() + "! You have called for SING UPs "
                    + singups + " times! You are required to post a karaoke video and ask an admin to reset your count to participate in turbos again.");
        }
        return true;
    }

    @Override
    public String help() {
        return null;
    }
}
