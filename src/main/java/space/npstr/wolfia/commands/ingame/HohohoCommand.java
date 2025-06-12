/*
 * Copyright (C) 2016-2025 the original author or authors
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

import java.util.List;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

@Command
public class HohohoCommand extends GameCommand {

    public static final String TRIGGER = "hohoho";

    public HohohoCommand(GameRegistry gameRegistry) {
        super(gameRegistry);
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("ho");
    }

    @Override
    public String help() {
        return invocation() + " name or number"
                + "\n#Send a present to a player. Make sure to use the player's number if the names are ambiguous.";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean execute(CommandContext commandContext) throws IllegalGameStateException {
        //this command is expected to be called by a player in a private channel

        //todo handle a player being part of multiple games properly
        boolean issued = false;
        boolean success = false;
        for (Game g : this.gameRegistry.getAll().values()) {
            if (g.isUserPlaying(commandContext.invoker)) {
                if (g.issueCommand(commandContext)) {
                    success = true;
                }
                issued = true;
            }
        }
        if (!issued) {
            commandContext.replyWithMention(String.format("you aren't playing in any game currently. Say `%s` to get started!",
                    WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER));
            return false;
        }
        return success;
    }
}
