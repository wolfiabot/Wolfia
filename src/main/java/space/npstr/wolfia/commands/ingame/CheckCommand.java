package space.npstr.wolfia.commands.ingame;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.GameCommand;

public class CheckCommand extends GameCommand {

    public CheckCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        return super.execute(commandInfo);
    }

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger() + " name or number"
                + "\n#Check the player. Make sure to use the player's number if the names are ambiguous";
    }

    @Override
    public boolean isCommandTrigger(final String command) {
        for (final String trigger : commandTriggers()) {
            if (trigger.equalsIgnoreCase(command)) {
                return true;
            }
        }
        return false;
    }
}
