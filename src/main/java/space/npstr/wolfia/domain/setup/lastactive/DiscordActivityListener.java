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

package space.npstr.wolfia.domain.setup.lastactive;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class DiscordActivityListener {

    private final ActivityService service;

    public DiscordActivityListener(ActivityService service) {
        this.service = service;
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onUserTyping(UserTypingEvent event) {
        active(event.getUser());
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        active(event.getAuthor());
    }

    private void active(User user) {
        this.service.recordActivity(user);
    }
}
