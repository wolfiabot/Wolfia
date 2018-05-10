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

import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;

/**
 * Created by napster on 14.12.17.
 */
public class OpenPresentCommand extends GameCommand {

    public OpenPresentCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + "\n#Open a present.";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws IllegalGameStateException {
        //this command is expected to be called by a player in a private channel

        //todo handle a player being part of multiple games properly
        boolean issued = false;
        boolean success = false;
        for (final Game g : Games.getAll().values()) {
            if (g.isUserPlaying(commandContext.invoker)) {
                if (g.issueCommand(commandContext)) {
                    success = true;
                }
                issued = true;
            }
        }
        if (!issued) {
            commandContext.replyWithMention(String.format("you aren't playing in any game currently. Say `%s` to get started!",
                    WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_HELP));
            return false;
        }
        return success;
    }

}
