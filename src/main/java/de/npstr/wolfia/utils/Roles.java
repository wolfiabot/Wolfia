package de.npstr.wolfia.utils;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.PermissionOverride;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.managers.RoleManager;

/**
 * Created by npstr on 18.11.2016
 * <p>
 * This class is there to easy handling roles, like their creation, assignment to players, and granting and denying rights
 */
public class Roles {

    /**
     * @param g    Guild aka Server where the bot operates and where from the role shall be retrieved/created
     * @param name Name of the role that shall be retrieved/dreated
     * @return Returns a role with the required name
     */
    public static Role getOrCreateRole(Guild g, String name) {

        RoleManager rm = null;
        for (Role r : g.getRolesByName(name)) {
            if (r.getName().equals(name)) {
                rm = r.getManager();
                break;
            }
        }
        if (rm == null) {
            rm = g.createRole();
            rm.setName(name);
        }
        rm.update();
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
        PermissionOverride po = channel.getOverrideForRole(rm.getRole());
        PermissionOverrideManager pom;
        if (po == null) pom = channel.createPermissionOverride(r);
        else pom = po.getManager();

        if (grant) pom.grant(p);
        else pom.deny(p);

        pom.update();
    }

    /**
     * @param g    Guild where the Role shall be deleted
     * @param name Name of the Role to be deleted. All roles with this name will be deleted
     */
    public static void deleteRole(Guild g, String name) {
        for (Role r : g.getRolesByName(name)) {
            if (r.getName().equals(name)) {
                r.getManager().delete();
            }
        }
    }

}
