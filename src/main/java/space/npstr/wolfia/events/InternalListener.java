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
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.entities.EGuild;
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

        final Guild guild = event.getGuild();
        try {
            final EGuild guildEntity = Wolfia.getDbWrapper().getOrCreate(guild.getIdLong(), EGuild.class);
            if (guildEntity.isPresent()) { //safeguard against discord shitting itself and spamming these for established guilds
                log.warn("Joined a guild that is marked as present. Not taking any further action");
                return;
            }

            guildEntity.set(guild).join().save();
        } catch (final DatabaseException e) {
            log.error("Db blew up while saving join event for guild {}", guild.getIdLong(), e);
        }
        DiscordLogger.getLogger().log("%s `%s` Joined guild %s with %s users.",
                Emojis.CHECK, TextchatUtils.berlinTime(), guild.getName(), guild.getMembers().size());
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        Listings.postToBotsDiscordPw(event.getJDA());
        Listings.postToDiscordbotsOrg(event.getJDA());

        final Guild guild = event.getGuild();
        try {
            EGuild.load(Wolfia.getDbWrapper(), guild.getIdLong()).set(guild).leave().save();
        } catch (final DatabaseException e) {
            log.error("Db blew up while saving leave event for guild {}", guild.getIdLong(), e);
        }

        int gamesDestroyed = 0;
        //destroy games running in the server that was left
        for (final Game game : Games.getAll().values()) {
            if (game.getGuildId() == guild.getIdLong()) {
                try {
                    game.destroy(new UserFriendlyException("Bot was kicked from the server " + guild.getName() + " " + guild.getIdLong()));
                    gamesDestroyed++;
                } catch (final Exception e) {
                    log.error("Exception when destroying a game in channel `{}` after leaving guild `{}`",
                            game.getChannelId(), guild.getIdLong(), e);
                }
            }
        }

        DiscordLogger.getLogger().log("%s `%s` Left guild %s with %s users, destroyed **%s** games.",
                Emojis.X, TextchatUtils.berlinTime(), guild.getName(), guild.getMembers().size(), gamesDestroyed);
    }

    @Override
    public void onTextChannelDelete(final TextChannelDeleteEvent event) {
        final long channelId = event.getChannel().getIdLong();
        final long guildId = event.getGuild().getIdLong();

        if (Games.get(channelId) != null) {
            DiscordLogger.getLogger().log("%s `%s` Destroying game due to deleted channel **#%s** `%s` in guild **%s** `%s`.",
                    Emojis.BOOM, TextchatUtils.berlinTime(),
                    event.getChannel().getName(), channelId, event.getGuild().getName(), guildId);

            Games.get(channelId).destroy(new UserFriendlyException("Main game channel `%s` in guild `%s` was deleted", channelId, guildId));
        }
    }
}
