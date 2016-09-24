package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.CommandListener;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 14.09.2016
 */
public class StartCommand extends Command {

    public StartCommand(CommandListener listener) {
        super(listener);
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return false;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        return false;
    }

    @Override
    public String help() {
        return null;
    }
}
