package space.npstr.wolfia.commands.debug;

import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.entities.Hstore;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;

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

    @Nonnull
    @Override
    public String help() {
        return "Set the maintenance flag";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context)
            throws IllegalGameStateException, DatabaseException {
        flipMaintenanceFlag();
        context.replyWithMention("set the maintenance flag to **" + getMaintenanceFlag() + "**");
        return true;
    }

    public static boolean getMaintenanceFlag() throws DatabaseException {
        return Boolean.valueOf(Hstore.loadAndGet(Wolfia.getDatabase().getWrapper(), MAINTENANCE_FLAG, Boolean.FALSE.toString()));
    }

    public static void flipMaintenanceFlag() throws DatabaseException {
        Hstore.loadApplyAndSave(Wolfia.getDatabase().getWrapper(), hstore -> {
            final String maintenance = hstore.get(MAINTENANCE_FLAG, Boolean.FALSE.toString());
            return hstore.set(MAINTENANCE_FLAG, Boolean.toString(!Boolean.valueOf(maintenance)));
        });
    }
}
