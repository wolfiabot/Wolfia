package de.npstr.wolfia;

import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 23.08.2016
 */
public interface Command {

    //this is called to check whether the arguments the user provided are ok
    public boolean argumentsValid(String[] args, MessageReceivedEvent event);

    //executes the command
    public boolean execute(String[] args, MessageReceivedEvent event);

    //return a help string that should explain the usage of this command
    public String help();

    //this handles output after the execution
    public void executed(boolean success, MessageReceivedEvent event);
}
