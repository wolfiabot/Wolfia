package de.npstr.wolfia.pregame.commands;

import de.npstr.wolfia.Command;
import de.npstr.wolfia.Main;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

import java.util.Map;

/**
 * Created by npstr on 09.09.2016
 */
public class HelpCommand extends Command {

    public final static String COMMAND = "help";
    private final static String HELP = "```usage: <prefix>help to see all available commands for this channel or <prefix>help <command> to see the help for a specific command```";

    private final Map<String, Command> commands;

    public HelpCommand(Map<String, Command> commands) {
        this.commands = commands;
    }

    @Override
    public boolean argumentsValid(String[] args, MessageReceivedEvent event) {
        if (args.length > 0) {
            if (commands.get(args[0]) == null)
                return false;
        }
        return true;
    }

    @Override
    public boolean execute(String[] args, MessageReceivedEvent event) {
        String out;
        if (args.length < 1) {
            out = "Available commands in this channel:\n```";
            for (String s : commands.keySet()) out += s + ", ";
            if (commands.size() > 0) out = out.substring(0, out.length() - 2);
            out += "```";
        } else {
            out = commands.get(args[0]).help();
        }
        Main.handleOutputMessage(event.getTextChannel(), out);
        return true;
    }

    @Override
    public String help() {
        return HELP;
    }
}
