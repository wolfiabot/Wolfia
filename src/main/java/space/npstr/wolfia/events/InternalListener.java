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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.App;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.EGuild;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.listing.Listings;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;

/**
 * Created by napster on 23.07.17.
 * <p>
 * Events listened to in here are used for bot internal, non-game purposes
 */
public class InternalListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(InternalListener.class);

    @Override
    public void onReady(final ReadyEvent event) {
        Listings.postToBotsDiscordPw(event.getJDA());
        Listings.postToDiscordbotsOrg(event.getJDA());

        DiscordLogger.getLogger().log("%s `%s` Ready! %s",
                Emojis.ROCKET, TextchatUtils.berlinTime(), App.VERSION);
    }

    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        Listings.postToBotsDiscordPw(event.getJDA());
        Listings.postToDiscordbotsOrg(event.getJDA());

        final Guild g = event.getGuild();
        final EGuild guildEntity = DbWrapper.getOrCreateEntity(g.getIdLong(), EGuild.class);
        if (guildEntity.isPresent()) { //safeguard against discord shitting itself and spamming these for established guilds
            log.warn("Joined a guild that is marked as present");
            return;
        }

        guildEntity.join();
        DiscordLogger.getLogger().log("%s `%s` Joined guild %s with %s users.",
                Emojis.CHECK, TextchatUtils.berlinTime(), g.getName(), g.getMembers().size());
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        Listings.postToBotsDiscordPw(event.getJDA());
        Listings.postToDiscordbotsOrg(event.getJDA());

        final Guild g = event.getGuild();
        DbWrapper.getOrCreateEntity(g.getIdLong(), EGuild.class).leave();

        int gamesDestroyed = 0;
        //destroy games running in the server that was left
        for (final Game game : Games.getAll().values()) {
            if (game.getGuildId() == g.getIdLong()) {
                try {
                    game.destroy(new UserFriendlyException("Bot was kicked from the server " + g.getName() + " " + g.getIdLong()));
                    gamesDestroyed++;
                } catch (final Exception e) {
                    log.error("Exception when destroying a game in channel `{}` after leaving guild `{}`",
                            game.getChannelId(), g.getIdLong(), e);
                }
            }
        }

        DiscordLogger.getLogger().log("%s `%s` Left guild %s with %s users, destroyed **%s** games.",
                Emojis.X, TextchatUtils.berlinTime(), g.getName(), g.getMembers().size(), gamesDestroyed);
    }
}
