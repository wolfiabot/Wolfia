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
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.restaction.PermissionOverrideAction;

import java.util.Optional;

/**
 * Created by npstr on 18.11.2016
 * <p>
 * This class is there to easy handling roles, like their creation, assignment to players, and granting and denying rights
 */
public class RoleUtils {

    /**
     * @param guild Guild aka Server where the bot operates and where from the role shall be retrieved/created
     * @param name  Name of the role that shall be retrieved/created
     * @return Returns a role with the required name
     */
    public static Role getOrCreateRole(final Guild guild, final String name) {
        final Optional<Role> r = guild.getRolesByName(name, true).stream()
                .filter(role -> role.getName().equals(name)).findFirst();
        return r.orElseGet(() -> guild.getController().createRole().setName(name).complete());
    }


    private enum PERMISSION_ACTION {GRANT, DENY, CLEAR}

    //i personally allow this thing to be ugly
    private static RestAction setPermissionsInChannelForRoleOrMember(final Channel channel, final IPermissionHolder memberOrRole,
                                                                     final PERMISSION_ACTION action, final Permission... permissions) {
        final PermissionOverride po;
        if (memberOrRole instanceof Role) {
            po = channel.getPermissionOverride((Role) memberOrRole);
        } else {
            po = channel.getPermissionOverride((Member) memberOrRole);
        }

        RestAction ra = null;
        if (po != null) {
            switch (action) {
                case GRANT:
                    ra = po.getManager().grant(permissions);
                    break;
                case DENY:
                    ra = po.getManager().deny(permissions);
                    break;
                case CLEAR:
                    ra = po.getManager().clear(permissions);
                    break;
            }
        } else {
            PermissionOverrideAction poa;
            if (memberOrRole instanceof Role) {
                poa = channel.createPermissionOverride((Role) memberOrRole);
            } else {
                poa = channel.createPermissionOverride((Member) memberOrRole);
            }
            switch (action) {
                case GRANT:
                    poa = poa.setAllow(permissions);
                    break;
                case DENY:
                    poa = poa.setAllow(permissions);
                    break;
                case CLEAR:
                    //no need to do things here
                    break;
            }
            ra = poa;
        }
        return ra;
    }

    /**
     * @param channel     Channel where this role and permission should take effect
     * @param role        Role that will be granted/denied the permission
     * @param permissions Permission that shall be granted/denied to the role
     */
    public static RestAction grant(final Channel channel, final Role role, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, role, PERMISSION_ACTION.GRANT, permissions);
    }

    public static RestAction deny(final Channel channel, final Role role, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, role, PERMISSION_ACTION.DENY, permissions);
    }

    public static RestAction clear(final Channel channel, final Role role, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, role, PERMISSION_ACTION.CLEAR, permissions);
    }

    public static RestAction grant(final Channel channel, final Member member, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, member, PERMISSION_ACTION.GRANT, permissions);
    }

    public static RestAction deny(final Channel channel, final Member member, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, member, PERMISSION_ACTION.DENY, permissions);
    }

    public static RestAction clear(final Channel channel, final Member member, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, member, PERMISSION_ACTION.CLEAR, permissions);
    }

    /**
     * @param g    Guild where the Role shall be deleted
     * @param name Name of the Role to be deleted. All roles with this name will be deleted
     */
    public static void deleteRole(final Guild g, final String name) {
        for (final Role r : g.getRolesByName(name, true)) {
            if (r.getName().equals(name)) {
                r.getManager().getRole().delete().complete();
            }
        }
    }

}
