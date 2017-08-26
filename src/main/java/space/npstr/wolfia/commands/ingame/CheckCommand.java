package space.npstr.wolfia.commands.ingame;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.GameCommand;

public class CheckCommand extends GameCommand {

    public static final String COMMAND = "check";

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        return super.execute(commandInfo);
    }

    @Override
    public String help() {
        return Config.PREFIX + COMMAND + " number"
                + "\n#Check the player. Make sure to use the player's number.";
    }

    @Override
    public boolean isCommandTrigger(final String command) {
        return COMMAND.equalsIgnoreCase(command);
    }
}
