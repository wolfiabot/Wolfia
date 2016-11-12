package de.npstr.wolfia.PopcornGame.Commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.CommandListener;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 09.11.2016
 */
public class GiveGunCommand extends Command {

    public final static String COMMAND = "gun";
    private final String HELP = "```usage: " + getListener().getPrefix()
            + COMMAND + " <player>\nto give <player> the gun. This is not a voting, this happens immediately, so " +
            "remember to consult your teams opinion first.```";

    public GiveGunCommand(CommandListener listener) {
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
        return HELP;
    }
}
