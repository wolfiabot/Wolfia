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

package space.npstr.wolfia.webapi.guild;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import space.npstr.wolfia.domain.guild.RemoteGuildService;
import space.npstr.wolfia.domain.settings.ChannelSettings;
import space.npstr.wolfia.domain.settings.ChannelSettingsService;
import space.npstr.wolfia.domain.settings.GuildSettings;
import space.npstr.wolfia.domain.settings.GuildSettingsService;
import space.npstr.wolfia.webapi.WebUser;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/guild_settings")
public class GuildSettingsEndpoint extends GuildEndpoint {

    private final GuildSettingsService guildSettingsService;
    private final ChannelSettingsService channelSettingsService;

    public GuildSettingsEndpoint(RemoteGuildService remoteGuildService, ShardManager shardManager,
                                 GuildSettingsService guildSettingsService,
                                 ChannelSettingsService channelSettingsService) {

        super(remoteGuildService, shardManager);
        this.guildSettingsService = guildSettingsService;
        this.channelSettingsService = channelSettingsService;
    }

    @GetMapping("/{guildId}")
    public ResponseEntity<GuildSettingsVO> get(@PathVariable long guildId, @Nullable WebUser user) {
        WebContext context = assertGuildAccess(user, guildId);

        GuildSettings guildSettings = this.guildSettingsService.guild(guildId).getOrDefault();
        return ok(toValueObject(guildSettings, collectChannelSettings(context.member)));
    }

    @PostMapping(value = "/{guildId}/channel_settings/game_channel")
    public ResponseEntity<GuildSettingsVO> setGameChannels(
            @PathVariable long guildId,
            @RequestBody List<GameChannelRequest> body,
            @Nullable WebUser user
    ) {
        WebContext context = assertGuildAccess(user, guildId);

        for (var entry : body) {
            setGameChannel(context, entry.channelId(), entry.isGameChannel());
        }

        GuildSettings guildSettings = this.guildSettingsService.guild(context.guild.getIdLong()).getOrDefault();
        return ok(toValueObject(guildSettings, collectChannelSettings(context.member)));
    }

    @PostMapping(value = "/{guildId}/channel_settings/{channelId}/game_channel/{isGameChannel}")
    public ResponseEntity<GuildSettingsVO> setGameChannel(
            @PathVariable long guildId,
            @PathVariable long channelId,
            @PathVariable boolean isGameChannel,
            @Nullable WebUser user
    ) {
        WebContext context = assertGuildAccess(user, guildId);

        setGameChannel(context, channelId, isGameChannel);

        GuildSettings guildSettings = this.guildSettingsService.guild(context.guild.getIdLong()).getOrDefault();
        return ok(toValueObject(guildSettings, collectChannelSettings(context.member)));
    }

    private void setGameChannel(WebContext context, long channelId, boolean isGameChannel) {
        TextChannel textChannel = context.guild.getTextChannelById(channelId);
        if (textChannel == null) { //if the channel has been deleted, we might as well remove it without further checks
            this.channelSettingsService.channel(channelId).reset();
            return;
        }

        if (!context.member.hasPermission(textChannel, Permission.VIEW_CHANNEL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        ChannelSettingsService.Action action = this.channelSettingsService.channel(channelId);
        if (isGameChannel) {
            action.enableGameChannel();
        } else {
            action.disableGameChannel();
        }
    }

    private List<ChannelSettingsVO> collectChannelSettings(Member member) {
        List<TextChannel> textChannels = member.getGuild().getTextChannels();
        textChannels = textChannels.stream()
                .filter(textChannel -> member.hasPermission(textChannel, Permission.VIEW_CHANNEL))
                .collect(Collectors.toList());
        List<Long> textChannelIds = textChannels.stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toList());
        Map<Long, ChannelSettings> channelSettings = this.channelSettingsService.channels(textChannelIds)
                .getOrDefault().stream()
                .collect(Collectors.toMap(ChannelSettings::getChannelId, Function.identity()));

        return textChannels.stream()
                .map(textChannel -> {
                    boolean isGameChannel = channelSettings.computeIfAbsent(
                            textChannel.getIdLong(),
                            id -> this.channelSettingsService.channel(id).getOrDefault()
                    ).isGameChannel();

                    return ImmutableChannelSettingsVO.builder()
                            .discordId(textChannel.getIdLong())
                            .name(textChannel.getName())
                            .isGameChannel(isGameChannel)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private GuildSettingsVO toValueObject(GuildSettings guildSettings, List<ChannelSettingsVO> channelSettingsList) {
        return ImmutableGuildSettingsVO.builder()
                .discordId(guildSettings.getGuildId())
                .addAllChannelSettings(channelSettingsList)
                .build();
    }

}
