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
import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.entities.Banlist;
import space.npstr.wolfia.game.definitions.Scope;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
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

    @Nonnull
    @Override
    public String help() {
        return "Globally ban mentioned user from signing up for games.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) throws DatabaseException {

        //is the user allowed to do that?
        if (!context.isOwner()) {
            context.replyWithMention("you are not allowed to use the global banlist.");
            return false;
        }

        String option = "";
        if (context.hasArguments()) {
            option = context.args[0];
        }

        BanAction action = null;
        if (TextchatUtils.isTrue(option)) {
            action = BanAction.ADD;
        } else if (TextchatUtils.isFalse(option)) {
            action = BanAction.REMOVE;
        }

        if (action == null) {
            final String answer = String.format("you didn't provide a ban action. Use `%s` or `%s`.",
                    TextchatUtils.TRUE_TEXT.get(0), TextchatUtils.FALSE_TEXT.get(0));
            context.replyWithMention(answer);
            return false;
        }


        final List<User> mentionedUsers = context.msg.getMentionedUsers();

        final List<String> mentions = new ArrayList<>();
        mentions.addAll(mentionedUsers.stream().map(User::getAsMention).collect(Collectors.toList()));
        final String joined = String.join("**, **", mentions);
        if (action == BanAction.ADD) {
            for (final long userId : mentionedUsers.stream().map(ISnowflake::getIdLong).collect(Collectors.toList())) {
                //noinspection ResultOfMethodCallIgnored
                Banlist.load(userId)
                        .setScope(Scope.GLOBAL)
                        .save();
            }
            context.replyWithMention(String.format("added **%s** to the global ban list.", joined));
            return true;
        } else { //removing
            for (final long userId : mentionedUsers.stream().map(ISnowflake::getIdLong).collect(Collectors.toList())) {
                Wolfia.getDbWrapper().deleteEntity(EntityKey.of(userId, Banlist.class));
            }
            context.replyWithMention(String.format("removed **%s** from the global ban list.", joined));
            return true;
        }
    }

    private enum BanAction {
        ADD,
        REMOVE
    }
}
