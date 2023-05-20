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

package space.npstr.wolfia.commands.debug;

import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import static java.util.Objects.requireNonNull;

/**
 * List running games
 */
@Command
public class RunningCommand implements BaseCommand {

    private final GameRegistry gameRegistry;

    public RunningCommand(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return "running";
    }

    @Override
    public String help() {
        return "List all running games";
    }

    @Override
    public boolean execute(CommandContext context) {

        Map<Long, Game> games = this.gameRegistry.getAll();
        for (Game game : games.values()) {
            EmbedBuilder eb = game.getStatus();
            eb.addBlankField(false);

            ShardManager shardManager = context.getJda().getShardManager();
            TextChannel channel = requireNonNull(shardManager).getTextChannelById(game.getChannelId());
            String guildName = "Guild not found";
            String channelName = "Channel not found";
            if (channel != null) {
                channelName = "#" + channel.getName();
                Guild guild = channel.getGuild();
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
