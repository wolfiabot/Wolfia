/*
 * Copyright (C) 2017 Dennis Neufeld
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

package space.npstr.wolfia.utils;


import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.PermOverrideManager;
import net.dv8tion.jda.core.managers.RoleManager;

/**
 * Created by npstr on 18.11.2016
 * <p>
 * This class is there to easy handling roles, like their creation, assignment to players, and granting and denying rights
 */
public class Roles {

    /**
     * @param guild Guild aka Server where the bot operates and where from the role shall be retrieved/created
     * @param name  Name of the role that shall be retrieved/dreated
     * @return Returns a role with the required name
     */
    public static Role getOrCreateRole(Guild guild, String name) {

        RoleManager rm = null;
        for (Role r : guild.getRolesByName(name, true)) {
            if (r.getName().equals(name)) {
                rm = r.getManager();
                break;
            }
        }
        if (rm == null) {
            rm = guild.getController().createRole().complete().getManager();
            rm.setName(name).complete();
        }
        return rm.getRole();
    }

    /**
     * @param channel Channel where this role and permission should take effect
     * @param r       Role that will be granted/denied the permission
     * @param p       Permission that shall be granted/denien to the role
     * @param grant   true to grant, false to deny
     */
    public static void grant(Channel channel, Role r, Permission p, boolean grant) {
        RoleManager rm = r.getManager();
        PermissionOverride po = channel.getPermissionOverride(rm.getRole());
        PermOverrideManager pom;
        if (po == null) pom = channel.createPermissionOverride(r).complete().getManager();
        else pom = po.getManager();

        if (grant) pom.grant(p).complete();
        else pom.deny(p).complete();
    }

    /**
     * @param g    Guild where the Role shall be deleted
     * @param name Name of the Role to be deleted. All roles with this name will be deleted
     */
    public static void deleteRole(Guild g, String name) {
        for (Role r : g.getRolesByName(name, true)) {
            if (r.getName().equals(name)) {
                r.getManager().getRole().delete().complete();
            }
        }
    }

}
