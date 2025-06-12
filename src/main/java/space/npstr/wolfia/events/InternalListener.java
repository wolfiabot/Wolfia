/*
 * Copyright (C) 2016-2025 the original author or authors
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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.App;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;

/**
 * Events listened to in here are used for bot internal, non-game purposes
 */
@Component
public class InternalListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InternalListener.class);

    private final GameRegistry gameRegistry;

    public InternalListener(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @EventListener
    public void onReady(ReadyEvent event) {
        log.info("{} Ready! Version: {} Logged in as: {}", Emojis.ROCKET, App.VERSION, event.getJDA().getSelfUser().getName());
    }

    @EventListener
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        log.info("Joined guild {} with {} users.", guild.getName(), guild.getMembers().size());
    }

    @EventListener
    public void onGuildLeave(GuildLeaveEvent event) {
        Guild guild = event.getGuild();

        int gamesDestroyed = 0;
        //destroy games running in the server that was left
        for (Game game : this.gameRegistry.getAll().values()) {
            if (game.getGuildId() == guild.getIdLong()) {
                try {
                    game.destroy(new UserFriendlyException("Bot was kicked from the server " + guild.getName() + " " + guild.getIdLong()));
                    gamesDestroyed++;
                } catch (Exception e) {
                    log.error("Exception when destroying a game in channel `{}` after leaving guild `{}`",
                            game.getChannelId(), guild.getIdLong(), e);
                }
            }
        }

        log.info("Left guild {} with {} users, destroyed {} games.",
                guild.getName(), guild.getMembers().size(), gamesDestroyed);
    }

    @EventListener
    public void onTextChannelDelete(ChannelDeleteEvent event) {
        long channelId = event.getChannel().getIdLong();

        Game game = this.gameRegistry.get(channelId);
        if (game != null) {
            long guildId = event.getGuild().getIdLong();
            log.info("Destroying game due to deleted channel {} {} in guild {} {}.",
                    event.getChannel().getName(), channelId, event.getGuild().getName(), guildId);

            game.destroy(new UserFriendlyException("Main game channel `%s` in guild `%s` was deleted",
                    channelId, guildId));
        }
    }
}
