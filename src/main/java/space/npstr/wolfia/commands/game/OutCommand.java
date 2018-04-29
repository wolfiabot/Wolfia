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

package space.npstr.wolfia.commands.game;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.db.entities.Setup;
import space.npstr.wolfia.game.definitions.Games;

import javax.annotation.Nonnull;

/**
 * Created by npstr on 23.08.2016
 */
public class OutCommand extends BaseCommand {

    public OutCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " [@user]"
                + "\n#Remove you from the current signup list. Moderators can out other players by mentioning them.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws DatabaseException {

        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        if (Games.get(context.textChannel) != null) {
            context.replyWithMention("please sign up/sign out for the next game after the current one is over.");
            return false;
        }

        //check for private guilds where we dont want games to be started
        if (PrivateGuild.isPrivateGuild(context.guild)) {
            context.replyWithMention("you can't play games in a private guild.");
            return false;
        }

        //is this a forced out of a player by an moderator or the bot owner?
        if (!context.msg.getMentionedUsers().isEmpty()) {
            if (!context.member.hasPermission(context.textChannel, Permission.MESSAGE_MANAGE) && !context.isOwner()) {
                context.replyWithMention("you need to have the following permission in this channel to be able to out players: "
                        + "**" + Permission.MESSAGE_MANAGE.name() + "**");
                return false;
            } else {
                final Setup s = Wolfia.getDatabase().getWrapper().findApplyAndMerge(Setup.key(context.textChannel.getIdLong()),
                        setup -> {
                            for (final User u : context.msg.getMentionedUsers()) {
                                setup.outUser(u.getIdLong());
                            }
                            return setup;
                        });
                context.reply(s.getStatus());
                return true;
            }
        } else {
            if (Wolfia.getDatabase().getWrapper().getOrCreate(Setup.key(context.textChannel.getIdLong())).isInned(context.invoker.getIdLong())) {
                //handling a regular out
                Wolfia.getDatabase().getWrapper().findApplyAndMerge(Setup.key(context.textChannel.getIdLong()),
                        setup -> {
                            if (setup.outUser(context.invoker.getIdLong())) {
                                context.reply(setup.getStatus());
                            }
                            return setup;
                        });
                return true;
            }
        }
        return false;
    }

}
