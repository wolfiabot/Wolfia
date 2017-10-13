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
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.sqlstack.DatabaseException;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.db.entities.ChannelSettings;

import java.util.Arrays;
import java.util.List;

/**
 * Created by napster on 22.06.17.
 * <p>
 * sets up the bot (= discord related options, not game related ones), targets owner/admins of a guild
 */
public class ChannelSettingsCommand extends BaseCommand {

    public ChannelSettingsCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger() + " [key value]"
                + "\n#Change or show settings for this channel. Examples:"
                + "\n  " + Config.PREFIX + getMainTrigger() + " accessrole @Mafiaplayer"
                + "\n  " + Config.PREFIX + getMainTrigger() + " tagcooldown 3"
                + "\n  " + Config.PREFIX + getMainTrigger();
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws DatabaseException {
        final MessageReceivedEvent event = commandInfo.event;
        final TextChannel channel = event.getTextChannel();
        final Member invoker = event.getMember();
        final Guild guild = event.getGuild();

        //will not be null because it will be initialized with default values if there is none
        final ChannelSettings channelSettings = Wolfia.getInstance().dbWrapper.getOrCreate(channel.getIdLong(), ChannelSettings.class);


        if (commandInfo.args.length == 0) {
            Wolfia.handleOutputEmbed(channel, channelSettings.getStatus());
            return true;
        }

        //is the user allowed to do that?
        if (!invoker.hasPermission(channel, Permission.MESSAGE_MANAGE) && !App.isOwner(invoker)) {
            Wolfia.handleOutputMessage(channel, "%s, you need the following permission to edit the settings of this channel: %s",
                    invoker.getAsMention(), Permission.MESSAGE_MANAGE.getName());
            return false;
        }

        //at least 2 arguments?
        if (commandInfo.args.length < 2) {
            commandInfo.reply(formatHelp(invoker.getUser()));
            return false;
        }

        final String option = commandInfo.args[0];
        switch (option.toLowerCase()) {
            case "accessrole":
                final Role accessRole;
                if (event.getMessage().getMentionedRoles().size() > 0) {
                    accessRole = event.getMessage().getMentionedRoles().get(0);
                } else {
                    final String roleName = String.join(" ", Arrays.copyOfRange(commandInfo.args, 1, commandInfo.args.length)).trim();
                    final List<Role> rolesByName = guild.getRolesByName(roleName, true);
                    if ("everyone".equals(roleName)) {
                        accessRole = guild.getPublicRole();
                    } else if (rolesByName.isEmpty()) {
                        Wolfia.handleOutputMessage(channel, "%s, there is no such role in this guild.", invoker.getAsMention());
                        return false;
                    } else if (rolesByName.size() > 1) {
                        Wolfia.handleOutputMessage(channel, "%s, there is more than one role with that name in this guild.", invoker.getAsMention());
                        return false;
                    } else {
                        accessRole = rolesByName.get(0);
                    }
                }
                channelSettings.setAccessRoleId(accessRole.getIdLong())
                        .save();
                break;
            case "tagcooldown":
                try {
                    Long tagCooldown = Long.valueOf(commandInfo.args[1]);
                    if (tagCooldown < 0) {
                        tagCooldown = 0L;
                    }
                    channelSettings.setTagCooldown(tagCooldown)
                            .save();
                } catch (final NumberFormatException e) {
                    Wolfia.handleOutputMessage(channel, "%s, please use a number of minutes to set the tags cooldown.", invoker.getAsMention());
                    return false;
                }
                break;
            default:
                //didn't understand the input, will show the status quo
                Wolfia.handleOutputMessage(channel, "%s, I did not understand that input.", invoker.getAsMention());
                return false;
        }
        Wolfia.handleOutputEmbed(channel, channelSettings.getStatus());
        return true;
    }
}