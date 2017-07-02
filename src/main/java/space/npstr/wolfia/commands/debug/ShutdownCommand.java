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

import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 21.06.17.
 * <p>
 * Shut the bot down
 */
public class ShutdownCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "shutdown";
    private static final Logger log = LoggerFactory.getLogger(ShutdownCommand.class);

    private static boolean shutdownInitiated = false;

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        Wolfia.maintenanceFlag = true;
        Wolfia.handleOutputMessage(true, commandInfo.event.getTextChannel(),
                "%s, **%s** games are still running. Will shut down as soon as they are over.",
                commandInfo.event.getAuthor().getAsMention(), Games.getAll().size());

        shutdownAfterGamesAreDoneWithCode(0);
        return true;
    }

    public static synchronized void shutdownAfterGamesAreDoneWithCode(final int code) {
        if (shutdownInitiated) return;
        shutdownInitiated = true;

        Wolfia.executor.scheduleAtFixedRate(() -> {
            if (Games.getAll().size() <= 0) {
                Wolfia.shutdown(code);
            } else {
                log.info("{} games still running, waiting...", Games.getAll().size());
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public String help() {
        return "shuts down the bot";
    }
}