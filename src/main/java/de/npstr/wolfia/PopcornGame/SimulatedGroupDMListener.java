package de.npstr.wolfia.PopcornGame;

import de.npstr.wolfia.Main;
import de.npstr.wolfia.utils.Sneaky;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

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
        if (Sneaky.DEBUG) users.add(Sneaky.NPSTR_ID);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) {
            return;
        }

        //if sent from a user of this group in a private channel to the bot
        if (users.contains(event.getAuthor().getId()) && event.getAuthor().getPrivateChannel().getId().equals(event.getChannel().getId())) {
            //echo the message to the users of this group
            for (String userId : users) {
                if (userId.equals(event.getAuthor().getId())) continue; //skip the sender of the message
                PrivateChannel pChan = event.getJDA().getUserById(userId).getPrivateChannel();
                Main.handleOutputMessage(pChan, event.getAuthor().getUsername() + ": " + event.getMessage().getContent());
            }
        }
    }
}
