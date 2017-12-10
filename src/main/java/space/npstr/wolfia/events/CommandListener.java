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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.commands.CommandHandler;

/**
 * Created by npstr on 25.08.2016
 */
public class CommandListener extends ListenerAdapter {

    private final static Logger log = LoggerFactory.getLogger(CommandListener.class);

    public CommandListener() {
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        CommandHandler.handleMessage(event);
    }

    @Override
    public void onReady(final ReadyEvent event) {
        log.info("Logged in as: " + event.getJDA().getSelfUser().getName());
    }
}