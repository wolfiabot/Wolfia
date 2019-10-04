package space.npstr.wolfia.commands.debug;

import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.entities.Hstore;
import space.npstr.wolfia.Launcher;
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
@Component
public class MaintenanceCommand implements BaseCommand, IOwnerRestricted {

    @Override
    public String getTrigger() {
        return "maint";
    }

    public static final String MAINTENANCE_FLAG = "maintenanceFlag";

    @Nonnull
    @Override
    public String help() {
        return "Set the maintenance flag";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context)
            throws IllegalGameStateException {
        flipMaintenanceFlag();
        context.replyWithMention("set the maintenance flag to **" + getMaintenanceFlag() + "**");
        return true;
    }

    public static boolean getMaintenanceFlag() {
        return Boolean.valueOf(Hstore.loadAndGet(Launcher.getBotContext().getDatabase().getWrapper(), MAINTENANCE_FLAG, Boolean.FALSE.toString()));
    }

    public static void flipMaintenanceFlag() {
        Hstore.loadApplyAndSave(Launcher.getBotContext().getDatabase().getWrapper(), hstore -> {
            final String maintenance = hstore.get(MAINTENANCE_FLAG, Boolean.FALSE.toString());
            return hstore.set(MAINTENANCE_FLAG, Boolean.toString(!Boolean.valueOf(maintenance)));
        });
    }
}
