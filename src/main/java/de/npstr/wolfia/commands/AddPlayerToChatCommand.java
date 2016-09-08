package de.npstr.wolfia.commands;

import de.npstr.wolfia.Command;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 04.09.2016
 */
public class AddPlayerToChatCommand implements Command {
    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public String help() {
        return null;
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent event) {

    }
}
