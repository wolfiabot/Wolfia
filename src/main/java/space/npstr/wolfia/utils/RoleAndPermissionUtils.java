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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.game.definitions.Scope;

import java.util.*;

/**
 * Created by npstr on 18.11.2016
 * <p>
 * This class is there to easy handling roles, like their creation, assignment to players, and granting and denying rights
 */
public class RoleAndPermissionUtils {

    private static final Logger log = LoggerFactory.getLogger(RoleAndPermissionUtils.class);

    /**
     * @param guild Guild aka Server where the bot operates and where from the role shall be retrieved/created
     * @param name  Name of the role that shall be retrieved/created
     * @return Returns a role with the required name
     */
    public static RestAction<Role> getOrCreateRole(final Guild guild, final String name) {
        final Optional<Role> r = guild.getRolesByName(name, true).stream()
                .filter(role -> role.getName().equals(name)).findFirst();
        return r.<RestAction<Role>>map(role -> new RestAction.EmptyRestAction<>(Wolfia.jda, role))
                .orElseGet(() -> guild.getController().createRole().setName(name));
    }

    public static boolean hasPermissions(final Member member, final TextChannel channel, final Map<Scope, Permission> permissions) {
        final ArrayList<Permission> guildPerms = new ArrayList<>();
        final ArrayList<Permission> channelPerms = new ArrayList<>();
        permissions.forEach((scope, permission) -> {
            if (scope == Scope.GUILD) {
                guildPerms.add(permission);
            } else if (scope == Scope.CHANNEL) {
                channelPerms.add(permission);
            }
        });
        return member.hasPermission(guildPerms) && member.hasPermission(channel, channelPerms);
    }

    public static boolean hasPermission(final Member member, final TextChannel channel, final Scope scope, final Permission permission) {
        if (scope == Scope.GUILD) {
            return member.hasPermission(permission);
        } else if (scope == Scope.CHANNEL) {
            return member.hasPermission(channel, permission);
        } else {
            throw new IllegalArgumentException("Unknown permission scope: " + scope.name());
        }
    }


    private enum PERMISSION_ACTION {GRANT, DENY, CLEAR}

    //i personally allow this thing to be ugly
    private static RestAction setPermissionsInChannelForRoleOrMember(final Channel channel, final IPermissionHolder memberOrRole,
                                                                     final PERMISSION_ACTION action, final Permission... permissions) {
        //dont bitch about a nonexisting role/member
        if (memberOrRole == null) {
            log.warn("setPermissionsInChannelForRoleOrMember() called with a null member/role. Fix your code dude. " +
                    "Not gonna be a bitch about this and pretend nothing happened, you owe me a solid.");
            return new RestAction.EmptyRestAction<>(Wolfia.jda, null);
        }
        final PermissionOverride po;
        if (memberOrRole instanceof Role) {
            po = channel.getPermissionOverride((Role) memberOrRole);
        } else if (memberOrRole instanceof Member) {
            po = channel.getPermissionOverride((Member) memberOrRole);
        } else {
            log.warn("Unsupported class of IPermissionHolder detected: {}, returning an empty action" + memberOrRole);
            return new RestAction.EmptyRestAction<>(Wolfia.jda, null);
        }

        RestAction ra = null;
        if (po != null) {
            switch (action) {
                case GRANT:
                    //do nothing if the permission override already grants the permission
                    if (po.getAllowed().containsAll(Arrays.asList(permissions))) {
                        ra = new RestAction.EmptyRestAction<>(Wolfia.jda, null);
                    } else {
                        ra = po.getManager().grant(permissions);
                    }
                    break;
                case DENY:
                    //do nothing if the permission override already denies the permission
                    if (po.getDenied().containsAll(Arrays.asList(permissions))) {
                        ra = new RestAction.EmptyRestAction<>(Wolfia.jda, null);
                    } else {
                        ra = po.getManager().deny(permissions);
                    }
                    break;
                case CLEAR:
                    //if the permission override becomes empty as a result of clearing these permissions, delete it
                    final List<Permission> currentPerms = new ArrayList<>();
                    currentPerms.addAll(po.getDenied());
                    currentPerms.addAll(po.getAllowed());
                    currentPerms.removeAll(Arrays.asList(permissions));

                    if (currentPerms.isEmpty()) {
                        ra = po.delete();
                    } else {
                        ra = po.getManager().clear(permissions);
                    }
                    break;
            }
        } else {
            final PermissionOverrideAction poa;
            if (memberOrRole instanceof Role) {
                poa = channel.createPermissionOverride((Role) memberOrRole);
            } else {
                poa = channel.createPermissionOverride((Member) memberOrRole);
            }
            switch (action) {
                case GRANT:
                    ra = poa.setAllow(permissions);
                    break;
                case DENY:
                    ra = poa.setDeny(permissions);
                    break;
                case CLEAR:
                    //do nothing if we are trying to clear a nonexisting permission override
                    ra = new RestAction.EmptyRestAction<>(Wolfia.jda, null);
                    break;
            }
        }
        return ra;
    }

    /**
     * @param channel      Channel where this role and permission should take effect
     * @param memberOrRole Member or Role that will be granted/denied the permission
     * @param permissions  Permissions that shall be granted/denied to the member/role
     */
    public static RestAction grant(final Channel channel, final IPermissionHolder memberOrRole, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, memberOrRole, PERMISSION_ACTION.GRANT, permissions);
    }

    public static RestAction deny(final Channel channel, final IPermissionHolder memberOrRole, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, memberOrRole, PERMISSION_ACTION.DENY, permissions);
    }

    public static RestAction clear(final Channel channel, final IPermissionHolder memberOrRole, final Permission... permissions) {
        return setPermissionsInChannelForRoleOrMember(channel, memberOrRole, PERMISSION_ACTION.CLEAR, permissions);
    }

    public static RestAction<Void> deleteIfCleared(final PermissionOverride permissionOverride) {
        //remove the whole override if it doesnt actually override any permission anymore
        if (permissionOverride != null && permissionOverride.getAllowed().isEmpty() && permissionOverride.getDenied().isEmpty()) {
            return permissionOverride.delete();
        } else {
            return new RestAction.EmptyRestAction<>(Wolfia.jda, null);
        }
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
