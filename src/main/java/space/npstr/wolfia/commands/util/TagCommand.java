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
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.ChannelSettings;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

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

    public static final String COMMAND = "tag";

    @Override
    public String help() {
        return Config.PREFIX + COMMAND + " add/remove/[your message]"
                + "\n#Add or remove yourself from the taglist, or tag members who signed up for the taglist with an optional message. Examples:"
                + "\n  " + Config.PREFIX + COMMAND + " add"
                + "\n  " + Config.PREFIX + COMMAND + " remove"
                + "\n  " + Config.PREFIX + COMMAND + " WANT SUM GAME?";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {

        final MessageReceivedEvent event = commandInfo.event;
        final Guild guild = event.getGuild();
        final TextChannel channel = event.getTextChannel();
        final Member invoker = event.getMember();
        final ChannelSettings settings = DbWrapper.getOrCreateEntity(channel.getIdLong(), ChannelSettings.class);
        final Set<Long> tags = settings.getTags();

        String option = "";
        if (commandInfo.args.length > 0) {
            option = commandInfo.args[0];
        }

        TagAction action = TagAction.TAG;
        if (TextchatUtils.isTrue(option)) {
            action = TagAction.ADD;
        } else if (TextchatUtils.isFalse(option)) {
            action = TagAction.REMOVE;
        }

        if (action == TagAction.TAG) {

            if (Games.get(channel.getIdLong()) != null) {
                Wolfia.handleOutputMessage(channel, "%s, I will not post the tag list during an ongoing game.",
                        invoker.getAsMention());
                return false;
            }

            if (System.currentTimeMillis() - settings.getTagListLastUsed()
                    < TimeUnit.MINUTES.toMillis(settings.getTagCooldown())) {
                Wolfia.handleOutputMessage(channel, "%s, you need to wait at least %s minutes between calling " +
                        "the tag list.", invoker.getAsMention(), settings.getTagCooldown());
                return false;
            }

            //the tag can only be used by a user who is on the taglist himself
            if (!tags.contains(invoker.getUser().getIdLong()) &&
                    invoker.getRoles().stream().mapToLong(Role::getIdLong).noneMatch(tags::contains)) {
                Wolfia.handleOutputMessage(channel, "%s, you can't use the taglist when you aren't part of it " +
                        "yourself.", invoker.getAsMention());
                return false;
            }

            final List<StringBuilder> outs = new ArrayList<>();
            final String message = invoker.getAsMention() + " called the tag list.\n"
                    + TextchatUtils.defuseMentions(commandInfo.beheaded.replaceFirst(COMMAND, "").trim())
                    + "\n";
            StringBuilder out = new StringBuilder(message);
            outs.add(out);

            final Set<Long> cleanUp = new HashSet<>();
            for (final long id : tags) {
                //is it a mentionable role?
                String toAdd = "";
                final Role role = guild.getRoleById(id);
                if (role != null && role.isMentionable()) {
                    toAdd = role.getAsMention() + " ";
                }
                //is it a member of the guild?
                final Member member = guild.getMemberById(id);
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
            outs.forEach(sb -> Wolfia.handleOutputMessage(channel, "%s", sb.toString()));
            settings.usedTagList();
            DbWrapper.merge(settings);
            return true;
        }


        final List<User> mentionedUsers = event.getMessage().getMentionedUsers();
        final List<Role> mentionedRoles = event.getMessage().getMentionedRoles();

        //user signing up / removing themselves
        if (mentionedUsers.isEmpty() && mentionedRoles.isEmpty()) {
            if (action == TagAction.ADD) {
                if (tags.contains(invoker.getUser().getIdLong())) {
                    Wolfia.handleOutputMessage(channel, "%s, you are already on the tag list of this channel.",
                            invoker.getAsMention());
                    return false;
                } else {
                    settings.addTag(invoker.getUser().getIdLong());
                    DbWrapper.merge(settings);
                    Wolfia.handleOutputMessage(channel, "%s, you have been added to the tag list of this " +
                            "channel.", invoker.getAsMention());
                    return true;
                }
            } else { //removing
                if (!tags.contains(invoker.getUser().getIdLong())) {
                    Wolfia.handleOutputMessage(channel, "%s, you are already removed from the tag list of this " +
                            "channel.", invoker.getAsMention());
                    return false;
                } else {
                    settings.removeTag(invoker.getUser().getIdLong());
                    DbWrapper.merge(settings);
                    Wolfia.handleOutputMessage(channel, "%s, you have been removed from the tag list of this " +
                            "channel", invoker.getAsMention());
                    return true;
                }
            }

        } else { //user signing up other users/roles
            //is the user allowed to do that?
            if (!invoker.hasPermission(channel, Permission.MESSAGE_MANAGE) && !App.isOwner(invoker)) {
                Wolfia.handleOutputMessage(channel, "%s, you need the following permission in this channel to " +
                                "add or remove other users or roles from the taglist of this channel: %s",
                        invoker.getAsMention(), Permission.MESSAGE_MANAGE.getName());
                return false;
            }
            final List<String> mentions = new ArrayList<>();
            mentions.addAll(mentionedUsers.stream().map(User::getAsMention).collect(Collectors.toList()));
            mentions.addAll(mentionedRoles.stream().map(Role::getAsMention).collect(Collectors.toList()));
            final String joined = String.join("**, **", mentions);
            if (action == TagAction.ADD) {
                mentionedUsers.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::addTag);
                mentionedRoles.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::addTag);
                DbWrapper.merge(settings);
                Wolfia.handleOutputMessage(channel, "%s, added **%s** to the tag list.", invoker.getAsMention(),
                        joined);
                return true;
            } else { //removing
                mentionedUsers.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::removeTag);
                mentionedRoles.stream().mapToLong(ISnowflake::getIdLong).forEach(settings::removeTag);
                DbWrapper.merge(settings);
                Wolfia.handleOutputMessage(channel, "%s, removed **%s** from the tag list.",
                        invoker.getAsMention(), joined);
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
