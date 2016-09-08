package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Main;
import de.npstr.wolfia.Player;
import de.npstr.wolfia.pregame.Pregame;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by npstr on 23.08.2016
 */
public class InCommand implements Command {

    //TODO add argument so players can say for how long they want to in

    private final String HELP = "TODO";

    private Pregame pg;

    public InCommand(Pregame pg) {
        super();
        this.pg = pg;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        //check SING UP counter
        if (Player.getSingup(event.getAuthor().getId()) >= 100) {
            Main.handleOutputMessage(event.getTextChannel(), "Hold on champ. You have reached 100 SING UPs. Submit a " +
                    "karaoke video featuring yourself and ask an admin to reset your SING UP counter.");
            return false;
        }


        int timeForSignup;
        try {
            timeForSignup = Integer.valueOf(args[0]);
            if (timeForSignup > 12 * 60) timeForSignup = 12 * 60; //max sign up time 12h
        } catch (Exception e) {
            timeForSignup = 60;
        }
        pg.inPlayer(event.getAuthor().getId(), timeForSignup);

        return true;
    }

    @Override
    public String help() {
        return HELP;
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent event) {

    }
}
