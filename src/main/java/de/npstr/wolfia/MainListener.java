package de.npstr.wolfia;

import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by npstr on 25.08.2016
 */
public class MainListener extends ListenerAdapter {

    private static final String PREFIX = "!";

    private final static Logger LOG = LogManager.getLogger();

    private final Map<Channel, ListenerAdapter> channelListeners = new HashMap<>();

    public MainListener() {
        //TODO restore channelListeners from DB
        //TODO make sure they are being saved there in the first place
    }

    public void setListener(ListenerAdapter listener, Channel channel) {
        channelListeners.put(channel, listener);
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