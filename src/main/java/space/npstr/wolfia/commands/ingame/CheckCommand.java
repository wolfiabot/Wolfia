package space.npstr.wolfia.commands.ingame;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;

public class CheckCommand extends GameCommand {

    public CheckCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " name or number"
                + "\n#Check the player. Make sure to use the player's number if the names are ambiguous.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws IllegalGameStateException {
        //this command is expected to be called by a player in a private channel

        //todo handle a player being part of multiple games properly
        boolean issued = false;
        boolean success = false;
        for (final Game g : Games.getAll().values()) {
            if (g.isUserPlaying(commandContext.invoker)) {
                if (g.issueCommand(commandContext)) {
                    success = true;
                }
                issued = true;
            }
        }
        if (!issued) {
            commandContext.replyWithMention(String.format("you aren't playing in any game currently. Say `%s` to get started!",
                    Config.PREFIX + CommRegistry.COMM_TRIGGER_HELP));
            return false;
        }
        return success;
    }
}
