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

package space.npstr.wolfia.utils;

import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Main;

import java.util.Set;

/**
 * Created by npstr on 05.11.2016
 * <p>
 * This class simulates a group dm in Discord
 * Users part of the group may write a PM to the bot and the bot will echo the message to the other participants
 */
public class SimulatedGroupDMListener extends ListenerAdapter {

    private Set<String> users;

    /**
     * @param users the people that should receive the simulated group dm
     */
    public SimulatedGroupDMListener(Set<String> users) {
        super();
        this.users = users;
        if (Config.C.isDebug) users.add(Config.NAPSTER_ID);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }

        //if sent from a user of this group in a private channel to the bot
        if (users.contains(event.getAuthor().getId()) && event.getAuthor().getPrivateChannel().getId().equals(event.getChannel().getId())) {
            //echo the message to the users of this group
            for (String userId : users) {
                if (userId.equals(event.getAuthor().getId())) continue; //skip the sender of the message
                PrivateChannel pChan = event.getJDA().getUserById(userId).getPrivateChannel();
                Main.handleOutputMessage(pChan, event.getAuthor().getName() + ": " + event.getMessage().getContent());
            }
        }
    }
}
