package de.npstr.wolfia.pregame;

import de.npstr.wolfia.Main;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by npstr on 23.08.2016
 */
public class PregameListener extends ListenerAdapter {

    private static final String PREFIX = "!";
    private final Pregame pregame;
    private final TextChannel channel;
    private final static Logger LOG = LogManager.getLogger();


    public PregameListener(Pregame pg) {
        super();
        pregame = pg;
        channel = pg.getChannel();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) {
            return;
        }

        if (event.getMessage().getContent().startsWith(PREFIX)) {
            pregame.handleCommand(Main.parser.parse(PREFIX, event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        LOG.trace("PregameListener running");
    }
}
