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

import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.UserFriendlyException;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Created by napster on 24.07.17.
 */
public class KillGameCommand extends BaseCommand implements IOwnerRestricted {

    public KillGameCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return "Stop and destroy an ongoing game.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) throws IllegalGameStateException {

        if (!context.hasArguments()) {
            context.reply("Please provide the channelId of the game you want to kill.");
            return false;
        }

        final long channelId;
        try {
            channelId = Long.parseLong(context.args[0]);
        } catch (final NumberFormatException e) {
            context.reply("Invalid channelId provided (not a long)");
            return false;
        }

        final Game game = Games.get(channelId);
        if (game == null) {
            context.reply("There is no game registered for channel " + channelId);
            return false;
        }

        String reason = String.join(" ", Arrays.copyOfRange(context.args, 1, context.args.length)).trim();
        if (reason.isEmpty()) reason = "Game killed by bot owner.";
        game.destroy(new UserFriendlyException(reason));

        context.reply("Game in channel " + channelId + " destroyed.");
        return true;
    }
}
