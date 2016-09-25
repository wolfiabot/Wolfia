package de.npstr.wolfia;

import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.Player;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.GenericMessageEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by npstr on 25.08.2016
 */
class MainListener extends ListenerAdapter implements CommandListener {

    private static final String PREFIX = "!";

    private final static Logger LOG = LogManager.getLogger();

    //keeps track of last activity of a user
    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
        if (event instanceof GenericMessageEvent) {
            User u = ((GenericMessageEvent) event).getAuthor();
            if (u != null) {
                Player.justSeen(u.getId());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) {
            return;
        }

        //does the message have our prefix?
        if (event.getMessage().getContent().startsWith(PREFIX)) {
            Main.handleCommand(CommandParser.parse(PREFIX, event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOG.trace("Logged in as: " + event.getJDA().getSelfInfo().getUsername());
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}