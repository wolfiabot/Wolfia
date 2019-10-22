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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandHandler;

/**
 * Created by npstr on 25.08.2016
 */
@Component
public class CommandListener extends ListenerAdapter {

    private final CommandHandler commandHandler;
    private final CommRegistry commRegistry;

    public CommandListener(CommandHandler commandHandler, final CommRegistry commRegistry) {
        this.commandHandler = commandHandler;
        this.commRegistry = commRegistry;
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        this.commandHandler.handleMessage(this.commRegistry, event);
    }
}
