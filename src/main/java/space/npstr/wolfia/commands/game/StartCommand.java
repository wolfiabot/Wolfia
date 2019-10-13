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

package space.npstr.wolfia.commands.game;

import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.GameStarter;
import space.npstr.wolfia.domain.room.PrivateRoomService;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;

/**
 * Created by npstr on 14.09.2016
 * <p>
 * any signed up player can use this command to start a game
 */
@Command
public class StartCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "start";

    private final GameStarter gameStarter;
    private final PrivateRoomService privateRoomService;

    public StartCommand(GameStarter gameStarter, PrivateRoomService privateRoomService) {
        this.gameStarter = gameStarter;
        this.privateRoomService = privateRoomService;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Nonnull
    @Override
    public String help() {
        return invocation()
                + "\n#Start the game. Game will only start if enough players have signed up.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws IllegalGameStateException {

        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        if (Games.get(context.textChannel) != null) {
            context.replyWithMention("please start the next game after the current one is over.");
            return false;
        }

        //check for private guilds where we dont want games to be started
        if (this.privateRoomService.guild(context.guild.getIdLong()).isPrivateRoom()) {
            context.replyWithMention("you can't play games in a private guild.");
            return false;
        }

        return this.gameStarter.startGame(context);
    }
}
