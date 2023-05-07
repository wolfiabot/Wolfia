/*
 * Copyright (C) 2016-2020 the original author or authors
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
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

/**
 * Game commands are different from regular commands as they can be registered by games.
 */
public abstract class GameCommand implements BaseCommand, PublicCommand {

    protected final GameRegistry gameRegistry;

    protected GameCommand(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @Override
    public boolean execute(CommandContext commandContext) throws IllegalGameStateException {
        GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        Game game = this.gameRegistry.get(context.textChannel);
        if (game == null) {
            //private guild?
            for (Game g : this.gameRegistry.getAll().values()) {
                if (context.guild.getIdLong() == g.getPrivateRoomGuildId()) {
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
