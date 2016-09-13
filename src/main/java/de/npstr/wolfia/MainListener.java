package de.npstr.wolfia;

import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.GenericMessageEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by npstr on 25.08.2016
 */
class MainListener extends ListenerAdapter {

    //private static final String PREFIX = "!";

    private final static Logger LOG = LogManager.getLogger();

    private final Map<Channel, ListenerAdapter> channelListeners = new HashMap<>();

    public MainListener() {
        //TODO restore channelListeners from DB
        //TODO make sure they are being saved there in the first place
    }

    public void addListener(ListenerAdapter listener, Channel channel) {
        channelListeners.put(channel, listener);
    }


    //keeps track of last activity of a user
    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
        if (event instanceof GenericMessageEvent) {
            User u = ((GenericMessageEvent) event).getAuthor();
            if (u != null) {
                Main.justSeen(u.getId());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) {
            return;
        }

        //filter messages by channel
        Channel targetChannel = event.getTextChannel();
        if (channelListeners.containsKey(targetChannel)) {
            channelListeners.get(targetChannel).onMessageReceived(event);
        }


//        if (event.getMessage().getContent().startsWith(PREFIX)) {
//            Main.handleCommand(Main.parser.parse(PREFIX, event.getMessage().getContent().toLowerCase(), event));
//        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOG.trace("Logged in as: " + event.getJDA().getSelfInfo().getUsername());
    }
}