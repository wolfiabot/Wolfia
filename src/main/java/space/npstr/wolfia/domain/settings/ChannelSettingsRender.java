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

package space.npstr.wolfia.domain.settings;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.commands.MessageContext;

import java.util.Optional;

@Component
public class ChannelSettingsRender {

    public MessageEmbed render(ChannelSettings settings) {
        final EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        long channelId = settings.getChannelId();
        final TextChannel channel = Launcher.getBotContext().getShardManager().getTextChannelById(channelId);
        if (channel == null) {
            eb.addField("Could not find channel with id " + channelId, "", false);
            return eb.build();
        }
        eb.setTitle("Settings for channel #" + channel.getName());
        eb.setDescription("Changes to the settings are reserved for channel moderators.");
        String roleName = "[Not set up]";
        Optional<Long> accessRoleId = settings.getAccessRoleId();
        if (accessRoleId.isPresent()) {
            final Role accessRole = channel.getGuild().getRoleById(accessRoleId.get());
            if (accessRole == null) {
                roleName = "[Deleted]";
            } else {
                roleName = accessRole.getName();
            }
        }
        eb.addField("Access Role", roleName, true);

        eb.addField("Tag list cooldown", settings.getTagCooldownMinutes() + " minutes", true);

        return eb.build();
    }
}
