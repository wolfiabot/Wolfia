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

import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.listing.Listings;

/**
 * Created by napster on 23.07.17.
 * <p>
 * Events listened to in here are used for internal purposes
 */
public class InternalListener extends ListenerAdapter {

    @Override
    public void onReady(final ReadyEvent event) {
        Listings.postToBotsDiscordPw();
        Listings.postToDiscordbotsOrg();
    }

    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        Listings.postToBotsDiscordPw();
        Listings.postToDiscordbotsOrg();
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        Listings.postToBotsDiscordPw();
        Listings.postToDiscordbotsOrg();
    }
}
