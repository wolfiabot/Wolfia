/*
 * Copyright (C) 2016-2020 the original author or authors
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.domain.settings.ChannelSettings;
import space.npstr.wolfia.domain.settings.ChannelSettingsService;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Allows users to sign up for a tag list
 */
@Command
public class TagCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "tag";

    private final ChannelSettingsService channelSettingsService;
    private final GameRegistry gameRegistry;

    public TagCommand(ChannelSettingsService channelSettingsService, GameRegistry gameRegistry) {
        this.channelSettingsService = channelSettingsService;
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " add/remove/[your message]"
                + "\n#Add or remove yourself from the taglist, or tag members who signed up for the taglist with an optional message. Examples:"
                + "\n  " + invocation() + " add"
                + "\n  " + invocation() + " remove"
                + "\n  " + invocation() + " WANT SUM GAME?";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) {

        final GuildCommandContext context = commandContext.requireGuild(false);
        if (context == null) {
            commandContext.reply("This is a private channel, there is noone in here to tag but us two ( ͡° ͜ʖ ͡°)");
            return false;
        }

        long channelId = context.textChannel.getIdLong();
        ChannelSettingsService.Action channelAction = this.channelSettingsService.channel(channelId);
        ChannelSettings channelSettings = channelAction.getOrDefault();
        final Set<Long> tags = channelSettings.getTags();

        String option = "";
        if (context.hasArguments()) {
            option = context.args[0];
        }

        TagAction action = TagAction.TAG;
        if (TextchatUtils.isTrue(option)) {
            action = TagAction.ADD;
        } else if (TextchatUtils.isFalse(option)) {
            action = TagAction.REMOVE;
        }

        if (action == TagAction.TAG) {

            if (this.gameRegistry.get(context.textChannel) != null) {
                context.replyWithMention("I will not post the tag list during an ongoing game.");
                return false;
            }

            long tagCooldownMinutes = channelSettings.getTagCooldownMinutes();
            if (System.currentTimeMillis() - channelSettings.getTagLastUsed()
                    < TimeUnit.MINUTES.toMillis(tagCooldownMinutes)) {
                final String answer = String.format("you need to wait at least %s minutes between calling the tag list.",
                        tagCooldownMinutes);
                context.replyWithMention(answer);
                return false;
            }

            //the tag can only be used by a user who is on the taglist himself
            if (!tags.contains(context.invoker.getIdLong())
                    && context.member.getRoles().stream().mapToLong(Role::getIdLong).noneMatch(tags::contains)) {
                context.replyWithMention(String.format("you can't use the taglist when you aren't part of it yourself. "
                        + "Say `%s` to add yourself to it.", WolfiaConfig.DEFAULT_PREFIX + TagCommand.TRIGGER + " +"));
                return false;
            }

            final List<StringBuilder> outs = new ArrayList<>();
            final String message = context.member.getAsMention() + " called the tag list.\n"
                    + TextchatUtils.defuseMentions(context.rawArgs).trim() + "\n";
            StringBuilder out = new StringBuilder(message);
            outs.add(out);

            final Set<Long> cleanUp = new HashSet<>();
            for (final long id : tags) {
                //is it a mentionable role?
                String toAdd = "";
                final Role role = context.guild.getRoleById(id);
                if (role != null && role.isMentionable()) {
                    toAdd = role.getAsMention() + " ";
                }
                //is it a member of the guild?
                final Member member = context.guild.getMemberById(id);
                if (member != null) {
                    toAdd = member.getAsMention() + " ";
                }
                if (!toAdd.isEmpty()) {
                    if (out.length() + toAdd.length() > TextchatUtils.MAX_MESSAGE_LENGTH) {
                        out = new StringBuilder();
                        outs.add(out);
                    }
                    out.append(toAdd);
                } else { //neither a mentionable role nor a member
                    cleanUp.add(id);
                }
            }

            channelAction.removeTags(cleanUp);
            for (final StringBuilder sb : outs) {
                context.reply(sb.toString());
            }
            channelAction.tagUsed();

            return true;
        }


        final List<User> mentionedUsers = context.msg.getMentionedUsers();
        final List<Role> mentionedRoles = context.msg.getMentionedRoles();

        //user signing up / removing themselves
        if (mentionedUsers.isEmpty() && mentionedRoles.isEmpty()) {
            if (action == TagAction.ADD) {
                if (tags.contains(context.invoker.getIdLong())) {
                    context.replyWithMention("you are already on the tag list of this channel.");
                    return false;
                } else {
                    channelAction.addTag(context.getInvoker().getIdLong());
                    context.replyWithMention("you have been added to the tag list of this channel.");
                    return true;
                }
            } else { //removing
                if (!tags.contains(context.invoker.getIdLong())) {
                    context.replyWithMention("you are already removed from the tag list of this channel.");
                    return false;
                } else {
                    channelAction.removeTag(context.getInvoker().getIdLong());
                    context.replyWithMention("you have been removed from the tag list of this channel");
                    return true;
                }
            }

        } else { //user signing up other users/roles
            //is the user allowed to do that?
            if (!context.member.hasPermission(context.textChannel, Permission.MESSAGE_MANAGE) && !context.isOwner()) {
                context.replyWithMention("you need the following permission in this channel to "
                        + "add or remove other users or roles from the taglist of this channel: "
                        + "**" + Permission.MESSAGE_MANAGE.getName() + "**");
                return false;
            }
            final List<String> mentions = Stream.concat(
                    mentionedUsers.stream().map(IMentionable::getAsMention),
                    mentionedRoles.stream().map(IMentionable::getAsMention)
            ).collect(Collectors.toList());
            final String joined = String.join("**, **", mentions);

            final List<Long> ids = Stream.concat(
                    mentionedUsers.stream().map(ISnowflake::getIdLong),
                    mentionedRoles.stream().map(ISnowflake::getIdLong)
            ).collect(Collectors.toList());

            if (action == TagAction.ADD) {
                channelAction.addTags(ids);
                context.replyWithMention(String.format("added **%s** to the tag list.", joined));
                return true;
            } else { //removing
                channelAction.removeTags(ids);
                context.replyWithMention(String.format("removed **%s** from the tag list.", joined));
                return true;
            }
        }
    }

    private enum TagAction {
        ADD,
        REMOVE,
        TAG
    }
}
