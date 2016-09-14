package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Listener;
import de.npstr.wolfia.pregame.Pregame;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 23.08.2016
 */
public class OutCommand extends Command {

    public static final String COMMAND = "out";
    private final String HELP = "```usage: " + getListener().getPrefix() + COMMAND + "\nwill remove you from the current signup list```";

    private final Pregame pg;

    public OutCommand(Listener l, Pregame pg) {
        super(l);
        this.pg = pg;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        pg.outPlayer(event.getAuthor().getId());
        return true;
    }

    @Override
    public String help() {
        return HELP;
    }

}
