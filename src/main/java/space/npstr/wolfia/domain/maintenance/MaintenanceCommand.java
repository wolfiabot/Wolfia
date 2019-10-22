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

package space.npstr.wolfia.domain.maintenance;

import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.domain.Command;

import javax.annotation.Nonnull;

/**
 * Created by napster on 14.07.17.
 * <p>
 * Sets the maintenance flag
 */
@Command
public class MaintenanceCommand implements BaseCommand {

    public final MaintenanceService service;

    public MaintenanceCommand(MaintenanceService maintenanceService) {
        this.service = maintenanceService;
    }

    @Override
    public String getTrigger() {
        return "maint";
    }

    @Nonnull
    @Override
    public String help() {
        return "Flip the maintenance flag";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) {
        this.service.flipMaintenanceFlag();
        context.replyWithMention("set the maintenance flag to **" + this.service.getMaintenanceFlag() + "**");
        return true;
    }
}
