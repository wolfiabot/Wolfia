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

package space.npstr.wolfia.domain.settings;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by napster on 22.06.17.
 * <p>
 * sets up the bot (= discord related options, not game related ones), targets owner/admins of a guild
 */
@Command
public class ChannelSettingsCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "channelsettings";

    private static final String AUTO_OUT = "auto-out";
    private static final String ENABLE = "enable";
    private static final Set<String> ENABLE_WORDS = Set.of(
            ENABLE, "true", "on"
    );
    private static final String DISABLE = "disable";
    private static final Set<String> DISABLE_WORDS = Set.of(
            DISABLE, "false", "off"
    );


    private final ChannelSettingsService service;
    private final ChannelSettingsRender render;

    public ChannelSettingsCommand(ChannelSettingsService service, ChannelSettingsRender render) {
        this.service = service;
        this.render = render;
    }

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

        long channelId = context.textChannel.getIdLong();
        ChannelSettingsService.Action channelAction = this.service.channel(channelId);
        ChannelSettings channelSettings = channelAction.getOrDefault();

        if (!context.hasArguments()) {
            context.reply(this.render.render(channelSettings));
            return true;
        }

        //is the user allowed to do that?
        if (!context.member.hasPermission(context.textChannel, Permission.MESSAGE_MANAGE) && !context.isOwner()) {
            context.replyWithMention("you need the following permission to edit the settings of this channel: "
                    + "**" + Permission.MESSAGE_MANAGE.getName() + "**");
            return false;
        }

        if (context.args.length == 1 && "reset".equalsIgnoreCase(context.args[0])) {
            channelAction.reset();
            context.replyWithMention("channel settings have been reset.");
            return true;
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
                channelSettings = channelAction.setAccessRoleId(accessRole.getIdLong());
                break;
            case "tagcooldown":
                try {
                    final long tagCooldown = Math.max(0L, Long.parseLong(context.args[1]));
                    channelSettings = channelAction.setTagCooldown(tagCooldown);
                } catch (final NumberFormatException e) {
                    context.replyWithMention("please use a number of minutes to set the tags cooldown.");
                    return false;
                }
                break;
            case AUTO_OUT:
                String input = context.args[1];
                boolean enableAction;
                if (ENABLE_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(input))) {
                    enableAction = true;
                } else if (DISABLE_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(input))) {
                    enableAction = false;
                } else {
                    String common = "`" + WolfiaConfig.DEFAULT_PREFIX + TRIGGER + " " + AUTO_OUT + " ";
                    String enable = common + ENABLE + "`";
                    String disable = common + DISABLE + "`";
                    context.replyWithMention("I didn't quite get your input. Try saying " + enable + " or " + disable);
                    return false;
                }

                if (enableAction) {
                    channelSettings = channelAction.enableAutoOut();
                } else {
                    channelSettings = channelAction.disableAutoOut();
                }
                break;
            default:
                context.help();
                return false;
        }

        context.reply(this.render.render(channelSettings));
        return true;
    }
}
