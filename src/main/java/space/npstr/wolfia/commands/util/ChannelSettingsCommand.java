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

package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.db.entities.ChannelSettings;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Created by napster on 22.06.17.
 * <p>
 * sets up the bot (= discord related options, not game related ones), targets owner/admins of a guild
 */
@Component
public class ChannelSettingsCommand implements BaseCommand {

    public static final String TRIGGER = "channelsettings";

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("cs");
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " [key value]"
                + "\n#Change or show settings for this channel. Examples:"
                + "\n  " + invocation() + " accessrole @Mafiaplayer"
                + "\n  " + invocation() + " tagcooldown 3"
                + "\n  " + invocation();
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) {
        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        final EntityKey<Long, ChannelSettings> key = ChannelSettings.key(context.textChannel.getIdLong());
        //will not be null because it will be initialized with default values if there is none
        ChannelSettings channelSettings = Launcher.getBotContext().getDatabase().getWrapper().getOrCreate(key);


        if (!context.hasArguments()) {
            context.reply(channelSettings.getStatus());
            return true;
        }

        //is the user allowed to do that?
        if (!context.member.hasPermission(context.textChannel, Permission.MESSAGE_MANAGE) && !context.isOwner()) {
            context.replyWithMention("you need the following permission to edit the settings of this channel: "
                    + "**" + Permission.MESSAGE_MANAGE.getName() + "**");
            return false;
        }

        //at least 2 arguments?
        if (context.args.length < 2) {
            context.help();
            return false;
        }

        final String option = context.args[0];
        switch (option.toLowerCase()) {
            case "accessrole":
                final Role accessRole;
                if (!context.msg.getMentionedRoles().isEmpty()) {
                    accessRole = context.msg.getMentionedRoles().get(0);
                } else {
                    final String roleName = String.join(" ", Arrays.copyOfRange(context.args, 1, context.args.length)).trim();
                    final List<Role> rolesByName = context.guild.getRolesByName(roleName, true);
                    if ("everyone".equals(roleName)) {
                        accessRole = context.guild.getPublicRole();
                    } else if (rolesByName.isEmpty()) {
                        context.replyWithMention("there is no such role in this guild.");
                        return false;
                    } else if (rolesByName.size() > 1) {
                        context.replyWithMention("there is more than one role with that name in this guild, use a "
                                + "mention to let me know which one you mean.");
                        return false;
                    } else {
                        accessRole = rolesByName.get(0);
                    }
                }
                channelSettings = Launcher.getBotContext().getDatabase().getWrapper().findApplyAndMerge(key, cs -> cs.setAccessRoleId(accessRole.getIdLong()));
                break;
            case "tagcooldown":
                try {
                    final Long tagCooldown = Math.max(0L, Long.valueOf(context.args[1]));
                    channelSettings = Launcher.getBotContext().getDatabase().getWrapper().findApplyAndMerge(key, cs -> cs.setTagCooldown(tagCooldown));
                } catch (final NumberFormatException e) {
                    context.replyWithMention("please use a number of minutes to set the tags cooldown.");
                    return false;
                }
                break;
            default:
                context.help();
                return false;
        }

        context.reply(channelSettings.getStatus());
        return true;
    }
}
