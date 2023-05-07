/*
 * Copyright (C) 2016-2023 the original author or authors
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

import java.util.Arrays;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.utils.UserFriendlyException;

@Command
public class KillGameCommand implements BaseCommand {

    private final GameRegistry gameRegistry;

    public KillGameCommand(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return "killgame";
    }

    @Override
    public String help() {
        return "Stop and destroy an ongoing game.";
    }

    @Override
    public boolean execute(CommandContext context) {

        if (!context.hasArguments()) {
            context.reply("Please provide the channelId of the game you want to kill.");
            return false;
        }

        long channelId;
        try {
            channelId = Long.parseLong(context.args[0]);
        } catch (NumberFormatException e) {
            context.reply("Invalid channelId provided (not a long)");
            return false;
        }

        Game game = this.gameRegistry.get(channelId);
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
