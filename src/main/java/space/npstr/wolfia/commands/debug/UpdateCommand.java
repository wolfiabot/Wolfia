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

package space.npstr.wolfia.commands.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.Games;

/**
 * Created by napster on 28.05.17.
 * <p>
 * wait for games to be over and run an update
 */
public class UpdateCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "update";

    private static final Logger log = LoggerFactory.getLogger(UpdateCommand.class);


    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {
        if (Wolfia.restartFlag) {
            Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(),
                    "%s, restart flag has been set already. Ignoring your command.",
                    commandInfo.event.getAuthor().getAsMention());
            return;
        }
        Wolfia.restartFlag = true;

        boolean interrupted = false;
        Wolfia.handleOutputMessage(true, commandInfo.event.getTextChannel(),
                "%s, **%s** games are still running. Will update as soon as they are over.",
                commandInfo.event.getAuthor().getAsMention(), Games.getAll().size());
        while (Games.getAll().size() > 0 && !interrupted) {
            log.info("{} games still running, waiting...", Games.getAll().size());
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                interrupted = true;
            }
        }

        Wolfia.shutdown(2);
    }

    @Override
    public String help() {
        return "restarts and updates the bot";
    }
}
