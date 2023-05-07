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

import space.npstr.wolfia.ShutdownHandler;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;

@Command
public class RestartCommand implements BaseCommand {


    private final GameRegistry gameRegistry;
    private final ShutdownHandler shutdownHandler;

    private boolean reminded = false;

    public RestartCommand(GameRegistry gameRegistry, ShutdownHandler shutdownHandler) {
        this.gameRegistry = gameRegistry;
        this.shutdownHandler = shutdownHandler;
    }

    @Override
    public String getTrigger() {
        return "restart";
    }

    @Override
    public String help() {
        return "Restart the bot.";
    }

    @Override
    public boolean execute(CommandContext context) {

        if (this.shutdownHandler.isShuttingDown()) {
            context.replyWithName(String.format("restart has been queued already! **%s** games still running.",
                    this.gameRegistry.getRunningGamesCount()));
            return false;
        }

        if (!this.reminded) {
            context.replyWithMention("you have fucked up in the past so here's a reminder:" +
                    "\n - Did you update the config files?" +
                    "\n - Any database migration necessary/implemented?" +
                    "\n - Did you actually upload the updated code?" +
                    "\nJust run the command again if you're sure you have done everything." +
                    "\n\n_Yours, " + context.msg.getJDA().getSelfUser().getName() + "_");
            this.reminded = true;
            return false;
        }

        String message = String.format("**%s** games are still running. Will restart as soon as they are over.",
                this.gameRegistry.getRunningGamesCount());
        Runnable restart = () -> this.shutdownHandler.shutdown(ShutdownHandler.EXIT_CODE_RESTART);
        context.replyWithMention(message, __ -> new Thread(restart, "shutdown-thread").start());
        return true;
    }
}
