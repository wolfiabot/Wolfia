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

import net.dv8tion.jda.core.entities.Guild;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;

/**
 * Created by napster on 06.08.17.
 */
public class NightkillCommand extends GameCommand {

    public NightkillCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " name or number"
                + "\n#Vote a player for nightkill. Make sure to use the player's number if the names are ambiguous";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) {
        //the nightkill command will always be called from a private guild, and only one game is allowed to run in
        //a private guild at the time
        Game game = null;
        for (final Game g : Games.getAll().values()) {
            final Guild guild = context.getGuild();
            if (guild != null && guild.getIdLong() == g.getPrivateGuildId()) {
                game = g;
                break;
            }
        }

        if (game == null) {
            context.replyWithMention("there is no game currently going on in here.");
            return false;
        }

        try {
            return game.issueCommand(this, context);
        } catch (final IllegalGameStateException e) {
            context.reply(e.getMessage());
            return false;
        }
    }
}
