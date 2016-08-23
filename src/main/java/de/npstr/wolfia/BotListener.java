package de.npstr.wolfia;

import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

/**
 * Created by npstr on 23.08.2016
 */
public class BotListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContent().startsWith("~!") && event.getMessage().getAuthor().getId() != event.getJDA().getSelfInfo().getId()) {
            Wolfia.handleCommand(Wolfia.parser.parse(event.getMessage().getContent().toLowerCase(), event));
        }
        super.onMessageReceived(event);
    }

    @Override
    public void onReady(ReadyEvent event) {
        //Wolfia.log("status", "Logged in as: " + event.getJDA().getSelfInfo().getUsername());
        super.onReady(event);
    }
}
