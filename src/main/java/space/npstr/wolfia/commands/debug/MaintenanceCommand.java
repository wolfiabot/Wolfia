/*
 * Copyright (C) 2016-2019 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
