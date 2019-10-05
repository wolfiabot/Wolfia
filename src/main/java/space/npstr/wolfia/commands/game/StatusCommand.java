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

import org.springframework.stereotype.Component;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.entities.Setup;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by npstr on 24.08.2016
 * <p>
 * this command should display the status of whatever is happening in a channel currently
 * <p>
 * is there a game running, whats it's state?
 * if not, is there a setup created for this channel, whats the status here, inned players etc?
 */
@Component
public class StatusCommand implements BaseCommand {

    public static final String TRIGGER = "status";

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("st");
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + "\n#Post the current game status or sign up list.";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) {
        //this command may be called from any channel. if its a private channel, look for ongoing games of the invoker

        final GuildCommandContext context = commandContext.requireGuild(false);
        if (context != null) { // find game through guild / textchannel
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
                    context.reply(Launcher.getBotContext().getDatabase().getWrapper().getOrCreate(Setup.key(context.textChannel.getIdLong()))
                            .getStatus());
                    return true;
                }
            }
            context.reply(game.getStatus().build());
            return true;
        } else {//handle it being issued in a private channel
            //todo handle a player being part of multiple games properly
            boolean issued = false;
            for (final Game g : Games.getAll().values()) {
                if (g.isUserPlaying(commandContext.invoker)) {
                    commandContext.reply(g.getStatus().build());
                    issued = true;
                }
            }
            if (!issued) {
                commandContext.replyWithMention(String.format("you aren't playing in any game currently. Say `%s` to get started!",
                        WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER));
                return false;
            }
            return true;
        }
    }
}
