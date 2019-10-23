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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Created by napster on 24.07.17.
 * <p>
 * List running games
 */
@Command
public class RunningCommand implements BaseCommand {

    @Override
    public String getTrigger() {
        return "running";
    }

    @Nonnull
    @Override
    public String help() {
        return "List all running games";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) {

        final Map<Long, Game> games = Games.getAll();
        for (final Game game : games.values()) {
            final EmbedBuilder eb = game.getStatus();
            eb.addBlankField(false);

            ShardManager shardManager = context.getJda().getShardManager();
            final TextChannel channel = requireNonNull(shardManager).getTextChannelById(game.getChannelId());
            String guildName = "Guild not found";
            String channelName = "Channel not found";
            if (channel != null) {
                channelName = "#" + channel.getName();
                final Guild guild = channel.getGuild();
                guildName = guild.getName();
            }
            eb.addField("Guild & Channel", guildName + "\n" + channelName + "\n" + game.getChannelId(), true);

            eb.addField("Started", TextchatUtils.toBerlinTime(game.getStartTime()), true);
            if (channel != null)
                eb.addField("Invite", TextchatUtils.getOrCreateInviteLinkForGuild(channel.getGuild(), channel), true);

            context.reply(eb.build());
        }

        context.reply(String.format("%s games registered.", games.size()));
        return true;
    }
}
