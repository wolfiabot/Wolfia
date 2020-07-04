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

package space.npstr.wolfia.domain.setup;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.commands.Context;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Renders are responsible for rendering complex information to be displayable in the Discord client.
 * <p>
 * TODO: generify as a component? apply automatically to "replied" objects?
 */
@Component
public class GameSetupRender {

    private final GameRegistry gameRegistry;

    public GameSetupRender(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    public MessageEmbed render(GameSetup setup, Context context) {
        long channelId = setup.getChannelId();

        Games game = setup.getGame();
        GameInfo.GameMode mode = setup.getMode();
        GameInfo info = Games.getInfo(game);
        Set<Long> innedUsers = setup.getInnedUsers();

        var neb = NiceEmbedBuilder.defaultBuilder();
        ShardManager shardManager = context.getJda().getShardManager();
        TextChannel channel = requireNonNull(shardManager).getTextChannelById(channelId);
        if (channel == null) {
            neb.addField("Could not find channel with id " + channelId, "", false);
            return neb.build();
        }
        neb.setTitle("Setup for channel #" + channel.getName());
        neb.setDescription(this.gameRegistry.get(channelId) == null
                ? "Game has **NOT** started yet."
                : "Game has started.");

        //games
        var possibleGames = new StringBuilder();
        Arrays.stream(Games.values()).forEach(g ->
                possibleGames.append(g == game ? "`[x]` " : "`[ ]` ").append(g.textRep).append("\n"));
        neb.addField("Game", possibleGames.toString(), true);

        //modes
        var possibleModes = new StringBuilder();
        info.getSupportedModes().forEach(m ->
                possibleModes.append(mode.equals(m) ? "`[x]` " : "`[ ]` ").append(m).append("\n"));
        neb.addField("Mode", possibleModes.toString(), true);

        //day length
        neb.addField("Day length", setup.getDayLength().toMinutes() + " minutes", true);

        //accepted player numbers
        neb.addField("Accepted players",
                info.getAcceptablePlayerNumbers(mode),
                true);

        //inned players
        var inned = new NiceEmbedBuilder.ChunkingField("Inned players (" + innedUsers.size() + ")", true);
        List<String> formatted = innedUsers.stream()
                .map(userId -> TextchatUtils.userAsMention(userId) + ", ")
                .collect(Collectors.toList());
        if (!formatted.isEmpty()) {
            String lastOne = formatted.remove(formatted.size() - 1);
            lastOne = lastOne.substring(0, lastOne.length() - 2); //remove the last ", "
            formatted.add(lastOne);
        }
        inned.addAll(formatted);
        neb.addField(inned);

        return neb.build();
    }
}
