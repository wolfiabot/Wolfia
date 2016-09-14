package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Listener;
import de.npstr.wolfia.pregame.Pregame;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 24.08.2016
 */
public class SignUpStatusCommand extends Command {

    public static final String COMMAND = "signups";
    private final String HELP = "```usage: " + getListener().getPrefix() + COMMAND + "\nposts the current signup list```";

    private final Pregame pg;

    public SignUpStatusCommand(Listener l, Pregame pg) {
        super(l);
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
}
