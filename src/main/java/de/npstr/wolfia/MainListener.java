package de.npstr.wolfia;

import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

/**
 * Created by npstr on 25.08.2016
 */
public class MainListener extends ListenerAdapter {

    public static final String PREFIX = "!";

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) {
            return;
        }

        if (event.getMessage().getContent().startsWith(PREFIX)) {
            Main.handleCommand(Main.parser.parse(PREFIX, event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        Main.log(Main.LOG.TRACE, "Logged in as: " + event.getJDA().getSelfInfo().getUsername());
    }
}