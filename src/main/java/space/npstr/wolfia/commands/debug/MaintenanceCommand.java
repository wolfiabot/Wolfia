package space.npstr.wolfia.commands.debug;

import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.entity.Hstore;
import space.npstr.wolfia.game.IllegalGameStateException;

/**
 * Created by napster on 14.07.17.
 * <p>
 * Sets the maintenance flag
 */
public class MaintenanceCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "maint";

    public static final String MAINTENANCE_FLAG = "maintenanceFlag";

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        flipMaintenancFlag();
        Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(), "%s, set the maintenance flag to **%s**",
                commandInfo.event.getAuthor().getAsMention(), getMaintenanceFlag());
        return true;
    }

    public static boolean getMaintenanceFlag() {
        return Boolean.valueOf(Hstore.loadAndGet(MAINTENANCE_FLAG, Boolean.FALSE.toString()));
    }

    public static void flipMaintenancFlag() {
        final Hstore hstore = Hstore.load();
        final String maintenance = hstore.get(MAINTENANCE_FLAG, Boolean.FALSE.toString());
        hstore.set(MAINTENANCE_FLAG, Boolean.toString(!Boolean.valueOf(maintenance))).save();
    }

    @Override
    public String help() {
        return "Set the maintenance flag";
    }
}
