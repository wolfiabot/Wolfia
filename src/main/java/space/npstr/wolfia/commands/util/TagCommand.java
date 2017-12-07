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
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.db.entities.ChannelSettings;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by napster on 30.06.17.
 * <p>
 * Allows users to sign up for a tag list
 */
public class TagCommand extends BaseCommand {

    public TagCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
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
    public boolean execute(@Nonnull final CommandContext context)
            throws IllegalGameStateException, DatabaseException {

        if (context.channel.getType() != ChannelType.TEXT) {
            context.reply("This is a private channel, there is noone in here to tag but us two ( ͡° ͜ʖ ͡°)");
            return false;
        }
        final TextChannel channel = (TextChannel) context.channel;


        final ChannelSettings settings = ChannelSettings.load(channel.getIdLong());
        final Set<Long> tags = settings.getTags();

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

            if (Games.get(context.channel.getIdLong()) != null) {
                context.replyWithMention("I will not post the tag list during an ongoing game.");
                return false;
            }

            if (System.currentTimeMillis() - settings.getTagListLastUsed()
                    < TimeUnit.MINUTES.toMillis(settings.getTagCooldown())) {
                final String answer = String.format("you need to wait at least %s minutes between calling the tag list.",
                        settings.getTagCooldown());
                context.replyWithMention(answer);
                return false;
            }

            //the tag can only be used by a user who is on the taglist himself
            final Member m = context.getMember();
            if (!tags.contains(context.invoker.getIdLong()) && m != null &&
                    m.getRoles().stream().mapToLong(Role::getIdLong).noneMatch(tags::contains)) {
                context.replyWithMention("you can't use the taglist when you aren't part of it yourself.");//todo say w.tag + to add yourself to it
                return false;
            }

            final List<StringBuilder> outs = new ArrayList<>();
            final String message = context.invoker.getAsMention() + " called the tag list.\n"
                    + TextchatUtils.defuseMentions(context.rawArgs).trim() + "\n";
            StringBuilder out = new StringBuilder(message);
            outs.add(out);

            final Set<Long> cleanUp = new HashSet<>();
            for (final long id : tags) {
                //is it a mentionable role?
                String toAdd = "";
                final Role role = channel.getGuild().getRoleById(id);
                if (role != null && role.isMentionable()) {
                    toAdd = role.getAsMention() + " ";
                }
                //is it a member of the guild?
                final Member member = channel.getGuild().getMemberById(id);
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
            settings.removeTags(cleanUp);
            for (final StringBuilder sb : outs) {
                context.reply(sb.toString());
            }
            settings.usedTagList()
                    .save();
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
                    settings.addTag(context.invoker.getIdLong())
                            .save();
                    context.replyWithMention("you have been added to the tag list of this channel.");
                    return true;
                }
            } else { //removing
                if (!tags.contains(context.invoker.getIdLong())) {
                    context.replyWithMention("you are already removed from the tag list of this channel.");
                    return false;
                } else {
                    settings.removeTag(context.invoker.getIdLong())
                            .save();
                    context.replyWithMention("you have been removed from the tag list of this channel");
                    return true;
                }
            }

        } else { //user signing up other users/roles
            //is the user allowed to do that?
            final Member m = context.getMember();
            if (m == null || (!m.hasPermission(channel, Permission.MESSAGE_MANAGE) && !context.isOwner())) {
                context.replyWithMention("you need the following permission in this channel to "
                        + "add or remove other users or roles from the taglist of this channel: "
                        + "**" + Permission.MESSAGE_MANAGE.getName() + "**");
                return false;
            }
            final List<String> mentions = new ArrayList<>();
            mentions.addAll(mentionedUsers.stream().map(User::getAsMention).collect(Collectors.toList()));
            mentions.addAll(mentionedRoles.stream().map(Role::getAsMention).collect(Collectors.toList()));
            final String joined = String.join("**, **", mentions);
            if (action == TagAction.ADD) {
                mentionedUsers.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::addTag);
                mentionedRoles.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::addTag);
                settings.save();
                context.replyWithMention(String.format("added **%s** to the tag list.", joined));
                return true;
            } else { //removing
                mentionedUsers.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::removeTag);
                mentionedRoles.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::removeTag);
                settings.save();
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
