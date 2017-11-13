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

import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.definitions.Games;

/**
 * Created by napster on 21.06.17.
 * <p>
 * Shut the bot down
 */
public class ShutdownCommand extends BaseCommand implements IOwnerRestricted {

    public ShutdownCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return "Shut down the bot.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {

        if (Wolfia.isShuttingDown()) {
            commandInfo.replyWithName(String.format("shutdown has been queued already! **%s** games still running.",
                    Games.getRunningGamesCount()));
            return false;
        }

        Wolfia.handleOutputMessage(true, commandInfo.event.getTextChannel(),
                "%s, **%s** games are still running. Will shut down as soon as they are over.",
                commandInfo.event.getAuthor().getAsMention(), Games.getRunningGamesCount());

        new Thread(() -> Wolfia.shutdown(Wolfia.EXIT_CODE_SHUTDOWN), "shutdown-thread").start();
        return true;
    }
}
