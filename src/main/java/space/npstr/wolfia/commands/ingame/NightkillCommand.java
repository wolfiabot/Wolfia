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

package space.npstr.wolfia.commands.ingame;

import java.util.List;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

@Command
public class NightkillCommand extends GameCommand {

    public static final String TRIGGER = "nightkill";

    public NightkillCommand(GameRegistry gameRegistry) {
        super(gameRegistry);
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("nk");
    }

    @Override
    public String help() {
        return invocation() + " name or number"
                + "\n#Vote a player for nightkill. Make sure to use the player's number if the names are ambiguous";
    }

    @Override
    public boolean execute(CommandContext commandContext) throws IllegalGameStateException {
        GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        //the nightkill command is expected to be called from a private guild, and only one game is allowed to run in
        //a private guild at the time
        Game game = null;
        for (Game g : this.gameRegistry.getAll().values()) {
            if (context.guild.getIdLong() == g.getPrivateRoomGuildId()) {
                game = g;
                break;
            }
        }

        if (game == null) {
            context.replyWithMention("this command needs to be called from wolfchat/mafiachat!");
            return false;
        }

        return game.issueCommand(context);
    }
}
