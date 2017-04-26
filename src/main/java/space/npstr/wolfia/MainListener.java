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

package space.npstr.wolfia;

import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.utils.CommandParser;

/**
 * Created by npstr on 25.08.2016
 */
class MainListener extends ListenerAdapter implements CommandListener {

    private final String PREFIX = "!";

    private final static Logger log = LoggerFactory.getLogger(MainListener.class);


    private CommandHandler main;

    public MainListener(CommandHandler main) {
        super();
        this.main = main;
    }

    //keeps track of last activity of a user
    //TODO sort this out
//    @Override
//    public void onEvent(Event event) {
//        super.onEvent(event);
//        if (event instanceof GenericMessageEvent) {
//            User u = ((GenericMessageEvent) event).getAuthor();
//            if (u != null) {
//                Player.justSeen(u.getId());
//            }
//        }
//    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }

        //ignore channels from an ignore list, like those that have a game going

        //does the message have our prefix?
        if (event.getMessage().getContent().startsWith(PREFIX)) {
            main.handleCommand(CommandParser.parse(PREFIX, event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        log.trace("Logged in as: " + event.getJDA().getSelfUser().getName());
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}