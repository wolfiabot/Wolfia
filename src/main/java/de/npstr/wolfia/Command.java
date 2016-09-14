package de.npstr.wolfia;

import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 23.08.2016
 */
public abstract class Command {

    private final Listener listener;

    public Command(Listener listener) {
        this.listener = listener;
    }

    public Listener getListener() {
        return listener;
    }

    //this is called to check whether the arguments the user provided are ok
    public abstract boolean argumentsValid(String[] args, MessageReceivedEvent event);

    //executes the command
    public abstract boolean execute(String[] args, MessageReceivedEvent event);

    //return a help string that should explain the usage of this command
    public abstract String help();

    //this handles output after the execution
    public void executed(boolean success, MessageReceivedEvent event) {
        if (!success) {
            Main.handleOutputMessage(event.getTextChannel(), help());
        }
    }
}
