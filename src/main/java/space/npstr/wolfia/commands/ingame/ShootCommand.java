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

package space.npstr.wolfia.commands.ingame;

import java.util.List;
import javax.annotation.Nonnull;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

/**
 * A player shoots another player
 */
@Command
public class ShootCommand extends GameCommand {

    public static final String TRIGGER = "shoot";

    public ShootCommand(GameRegistry gameRegistry) {
        super(gameRegistry);
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("s", "blast");
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " @player"
                + "\n#Shoot the mentioned player.";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws IllegalGameStateException {
        //this command may be called in a guild for popcorn and a private channel for mafia

        final GuildCommandContext context = commandContext.requireGuild(false);
        if (context != null) { // find game through guild / textchannel
            Game game = this.gameRegistry.get(context.textChannel);
            if (game == null) {
                //private guild?
                for (final Game g : this.gameRegistry.getAll().values()) {
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
        } else {//handle it being issued in a private channel
            //todo handle a player being part of multiple games properly
            boolean issued = false;
            boolean success = false;
            for (final Game g : this.gameRegistry.getAll().values()) {
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
}
