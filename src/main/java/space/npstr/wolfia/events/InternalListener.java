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

package space.npstr.wolfia.events;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.listing.Listings;
import space.npstr.wolfia.utils.discord.Emojis;

/**
 * Created by napster on 23.07.17.
 * <p>
 * Events listened to in here are used for internal purposes
 * <p>
 * todo prevent spamming of these into the log channel during discord hickups
 */
public class InternalListener extends ListenerAdapter {

    @Override
    public void onReady(final ReadyEvent event) {
        Listings.postToBotsDiscordPw(event.getJDA());
        Listings.postToDiscordbotsOrg(event.getJDA());

        event.getJDA().getTextChannelById(Config.C.logChannelId).sendMessageFormat("%s Ready!", Emojis.ROCKET).queue();
    }

    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        Listings.postToBotsDiscordPw(event.getJDA());
        Listings.postToDiscordbotsOrg(event.getJDA());

        final Guild g = event.getGuild();
        Wolfia.handleOutputMessage(Config.C.logChannelId, "%s Joined guild %s with %s users.",
                Emojis.CHECK, g.getName(), g.getMembers().size());
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        Listings.postToBotsDiscordPw(event.getJDA());
        Listings.postToDiscordbotsOrg(event.getJDA());

        final Guild g = event.getGuild();
        Wolfia.handleOutputMessage(Config.C.logChannelId, "%s Left guild %s with %s users.",
                Emojis.X, g.getName(), g.getMembers().size());
    }
}
