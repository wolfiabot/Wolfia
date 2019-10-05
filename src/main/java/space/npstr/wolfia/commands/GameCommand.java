/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

package space.npstr.wolfia.commands;

import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;

/**
 * Created by napster on 21.05.17.
 * <p>
 * Game commands are different from regular commands as they can be registered by games.
 */
public abstract class GameCommand implements BaseCommand {

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws IllegalGameStateException {
        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        Game game = Games.get(context.textChannel);
        if (game == null) {
            //private guild?
            for (final Game g : Games.getAll().values()) {
                if (context.guild.getIdLong() == g.getPrivateGuildId()) {
                    game = g;
                    break;
                }
            }

            if (game == null) {
                context.replyWithMention(String.format("there is no game currently going on in here. Say `%s` to get started!",
                        WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER));
                return false;
            }
        }

        return game.issueCommand(context);
    }
}
