package de.npstr.wolfia.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Listener;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 04.09.2016
 */
public class AddPlayerToHiddenChannelCommand extends Command {

    public AddPlayerToHiddenChannelCommand(Listener listener) {
        super(listener);
    }

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

}
