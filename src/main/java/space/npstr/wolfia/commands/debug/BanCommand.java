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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.Banlist;
import space.npstr.wolfia.game.definitions.Scope;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by napster on 07.07.17.
 * <p>
 * Ban users from playing the game.
 */
public class BanCommand extends BaseCommand implements IOwnerRestricted {

    public BanCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return "Globally ban mentioned user from signing up for games.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {


        final MessageReceivedEvent event = commandInfo.event;
        final TextChannel channel = event.getTextChannel();
        final Member invoker = event.getMember();

        //is the user allowed to do that?
        if (!App.isOwner(invoker)) {
            Wolfia.handleOutputMessage(channel, "%s, you are not allowed to use the global banlist.",
                    invoker.getAsMention());
            return false;
        }

        String option = "";
        if (commandInfo.args.length > 0) {
            option = commandInfo.args[0];
        }

        BanAction action = null;
        if (TextchatUtils.isTrue(option)) {
            action = BanAction.ADD;
        } else if (TextchatUtils.isFalse(option)) {
            action = BanAction.REMOVE;
        }

        if (action == null) {
            Wolfia.handleOutputMessage(channel, "%s, you didn't provide a ban action. Use `%s` or `%s`.",
                    invoker.getAsMention(), TextchatUtils.TRUE_TEXT.get(0), TextchatUtils.FALSE_TEXT.get(0));
            return false;
        }


        final List<User> mentionedUsers = event.getMessage().getMentionedUsers();

        //user signing up other users/roles
        final List<String> mentions = new ArrayList<>();
        mentions.addAll(mentionedUsers.stream().map(User::getAsMention).collect(Collectors.toList()));
        final String joined = String.join("**, **", mentions);
        if (action == BanAction.ADD) {
            mentionedUsers.stream().mapToLong(ISnowflake::getIdLong).forEach(userId -> {
                final Banlist userBan = DbWrapper.getOrCreateEntity(userId, Banlist.class);
                userBan.setScope(Scope.GLOBAL);
                DbWrapper.merge(userBan);
            });
            Wolfia.handleOutputMessage(channel, "%s, added **%s** to the global ban list.", invoker.getAsMention(),
                    joined);
            return true;
        } else { //removing
            mentionedUsers.stream().mapToLong(ISnowflake::getIdLong).forEach(userId -> DbWrapper.deleteEntity(userId, Banlist.class));
            Wolfia.handleOutputMessage(channel, "%s, removed **%s** from the global ban list.",
                    invoker.getAsMention(), joined);
            return true;
        }
    }

    private enum BanAction {
        ADD,
        REMOVE
    }
}
