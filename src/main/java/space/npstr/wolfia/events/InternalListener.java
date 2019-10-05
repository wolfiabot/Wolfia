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

package space.npstr.wolfia.events;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.App;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;

/**
 * Created by napster on 23.07.17.
 * <p>
 * Events listened to in here are used for bot internal, non-game purposes
 */
public class InternalListener extends ListenerAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InternalListener.class);

    @Override
    public void onReady(final ReadyEvent event) {
        log.info("{} Ready! Version: {} Logged in as: {}", Emojis.ROCKET, App.VERSION, event.getJDA().getSelfUser().getName());
    }

    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        final Guild guild = event.getGuild();
        log.info("Joined guild {} with {} users.", guild.getName(), guild.getMembers().size());
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        final Guild guild = event.getGuild();

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

        log.info("Left guild {} with {} users, destroyed {} games.",
                guild.getName(), guild.getMembers().size(), gamesDestroyed);
    }

    @Override
    public void onTextChannelDelete(final TextChannelDeleteEvent event) {
        final long channelId = event.getChannel().getIdLong();
        final long guildId = event.getGuild().getIdLong();

        Game game = Games.get(channelId);
        if (game != null) {
            log.info("Destroying game due to deleted channel {} {} in guild {} {}.",
                    event.getChannel().getName(), channelId, event.getGuild().getName(), guildId);

            game.destroy(new UserFriendlyException("Main game channel `%s` in guild `%s` was deleted",
                    channelId, guildId));
        }
    }
}
