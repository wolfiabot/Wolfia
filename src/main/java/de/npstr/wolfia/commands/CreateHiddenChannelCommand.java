package de.npstr.wolfia.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Listener;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 04.09.2016
 */
public class CreateHiddenChannelCommand extends Command {

    private static final String HELP = "usage: <prefix>create";

    public CreateHiddenChannelCommand(Listener listener) {
        super(listener);
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && !args[0].equals("")) return true;

        return false;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public String help() {
        return HELP;
    }

}
