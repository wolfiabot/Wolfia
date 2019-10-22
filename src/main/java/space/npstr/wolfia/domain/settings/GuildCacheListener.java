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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;


/**
 * Possible improvement: Running the database writes async, as during reloads of shards there can be quite a few
 * ready events happening.
 */
@Component
public class GuildCacheListener extends ListenerAdapter {

    private final GuildSettingsService guildSettingsService;

    public GuildCacheListener(GuildSettingsService guildSettingsService) {
        this.guildSettingsService = guildSettingsService;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        dataUpdate(event.getGuild());
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        dataUpdate(event.getGuild());
    }

    @Override
    public void onGuildUpdateIcon(GuildUpdateIconEvent event) {
        dataUpdate(event.getGuild());
    }

    @Override
    public void onGuildUpdateName(GuildUpdateNameEvent event) {
        dataUpdate(event.getGuild());
    }

    private void dataUpdate(Guild guild) {
        this.guildSettingsService.set(guild);
    }
}
