package space.npstr.wolfia.commands.ingame;

import space.npstr.wolfia.commands.GameCommand;

import javax.annotation.Nonnull;

public class CheckCommand extends GameCommand {

    public CheckCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " name or number"
                + "\n#Check the player. Make sure to use the player's number if the names are ambiguous";
    }

    @Override
    public boolean isCommandTrigger(final String command) {
        for (final String trigger : this.aliases) {
            if (trigger.equalsIgnoreCase(command)) {
                return true;
            }
        }
        return false;
    }
}
