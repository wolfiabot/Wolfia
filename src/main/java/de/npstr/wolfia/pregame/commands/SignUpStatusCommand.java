package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.pregame.Pregame;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 24.08.2016
 */
public class SignUpStatusCommand implements Command {

    private final String HELP = "TODO";

    private Pregame pg;

    public SignUpStatusCommand(Pregame pg) {
        super();
        this.pg = pg;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        pg.postSignUps();
        return true;
    }

    @Override
    public String help() {
        return HELP;
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent event) {

    }
}
