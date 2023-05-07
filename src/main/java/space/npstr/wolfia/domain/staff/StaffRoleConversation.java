/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.domain.staff;

import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.domain.Conversation;
import space.npstr.wolfia.system.EventWaiter;

/**
 * Nothing to see here. This is just a placeholder for future staff command functionality / multileveled conversational commands.
 */
public class StaffRoleConversation implements Conversation {

    private final EventWaiter eventWaiter;

    public StaffRoleConversation(EventWaiter eventWaiter) {
        this.eventWaiter = eventWaiter;
    }

    @Override
    public EventWaiter getEventWaiter() {
        return this.eventWaiter;
    }

    @Override
    public boolean start(MessageContext context) {
        context.reply("https://media.giphy.com/media/vzvFGQs0P013i/giphy.gif");
        return true;
    }
}
