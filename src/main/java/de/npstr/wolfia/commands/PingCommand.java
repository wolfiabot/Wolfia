package de.npstr.wolfia.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Main;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 23.08.2016
 */
public class PingCommand implements Command {
    private final String HELP = "USAGE: ~!ping";

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        Main.handleOutputMessage(event.getTextChannel(), "PONG");
        return true;
    }

    @Override
    public String help() {
        return HELP;
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent event) {
        return;
    }
}
