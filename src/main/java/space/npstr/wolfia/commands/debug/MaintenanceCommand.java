package space.npstr.wolfia.commands.debug;

import space.npstr.sqlstack.DatabaseException;
import space.npstr.sqlstack.DatabaseWrapper;
import space.npstr.sqlstack.entities.Hstore;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

/**
 * Created by napster on 14.07.17.
 * <p>
 * Sets the maintenance flag
 */
public class MaintenanceCommand extends BaseCommand implements IOwnerRestricted {

    public MaintenanceCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    public static final String MAINTENANCE_FLAG = "maintenanceFlag";

    @Override
    public String help() {
        return "Set the maintenance flag";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException, DatabaseException {
        flipMaintenancFlag();
        Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(), "%s, set the maintenance flag to **%s**",
                commandInfo.event.getAuthor().getAsMention(), getMaintenanceFlag());
        return true;
    }

    public static boolean getMaintenanceFlag() throws DatabaseException {
        return Boolean.valueOf(Hstore.loadAndGet(Wolfia.getInstance().dbWrapper, MAINTENANCE_FLAG, Boolean.FALSE.toString()));
    }

    public static void flipMaintenancFlag() throws DatabaseException {
        final DatabaseWrapper dbWrapper = Wolfia.getInstance().dbWrapper;
        final Hstore hstore = Hstore.load(dbWrapper);
        final String maintenance = hstore.get(MAINTENANCE_FLAG, Boolean.FALSE.toString());
        hstore.set(MAINTENANCE_FLAG, Boolean.toString(!Boolean.valueOf(maintenance))).save();
    }
}
