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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.IllegalGameStateException;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.Map;

/**
 * Created by napster on 24.07.17.
 * <p>
 * List running games
 */
public class RunningCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "running";

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {

        //todo whenever jda supports multiple embeds make use of those
        final Map<Long, Game> games = Games.getAll();
        for (final Game game : games.values()) {
            final EmbedBuilder eb = game.getStatus();
            eb.addBlankField(false);

            final TextChannel channel = Wolfia.jda.getTextChannelById(game.getChannelId());
            String guildName = "Guild not found";
            String channelName = "Channel not found";
            if (channel != null) {
                channelName = "#" + channel.getName();
                final Guild guild = channel.getGuild();
                if (guild != null) guildName = guild.getName();
            }
            eb.addField("Guild & Channel", guildName + "\n" + channelName + "\n" + game.getChannelId(), true);

            eb.addField("Started", TextchatUtils.toBerlinTime(game.getStartTime()), true);
            if (channel != null)
                eb.addField("Invite", TextchatUtils.getOrCreateInviteLink(channel), true);

            commandInfo.reply(eb.build());
        }

        commandInfo.reply(String.format("%s games registered.", games.size()));
        return true;
    }

    @Override
    public String help() {
        return "todo"; // todo
    }
}
