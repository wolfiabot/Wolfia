package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.CommandListener;
import de.npstr.wolfia.pregame.Pregame;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 22.10.2016
 */
public class ConfirmCommand extends Command {

    public final static String COMMAND = "confirm";
    private final String HELP = "```usage: " + getListener().getPrefix()
            + COMMAND + " \nto start the game. Game will only start if enough players have signed up\n";

    private Pregame pg;

    public ConfirmCommand(CommandListener listener, Pregame pg) {
        super(listener);
        this.pg = pg;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        pg.confirm(event.getAuthor().getId());
        return false;
    }

    @Override
    public String help() {
        return HELP;
    }
}
