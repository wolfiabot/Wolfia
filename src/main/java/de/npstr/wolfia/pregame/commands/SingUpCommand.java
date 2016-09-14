package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Listener;
import de.npstr.wolfia.Main;
import de.npstr.wolfia.utils.Player;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 06.09.2016
 */
public class SingUpCommand extends Command {

    public static final String COMMAND = "singups";

    public SingUpCommand(Listener listener) {
        super(listener);
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        int singups = Player.singup(event.getAuthor().getId());
        if (singups < 10) {
            Main.handleOutputMessage(event.getTextChannel(), event.getAuthor().getAsMention() + " called for SING UPs "
                    + singups + " times! He is required to submit a karaoke video at 10 SING UPs :)");
        } else {
            Main.handleOutputMessage(event.getTextChannel(), "Congratulations, " + event.getAuthor().getAsMention() + "! You have called for SING UPs "
                    + singups + " times! You are required to post a karaoke video and ask an admin to reset your count to participate in turbos again.");
        }
        return true;
    }

    @Override
    public String help() {
        return null;
    }
}
