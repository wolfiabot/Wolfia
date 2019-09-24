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
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.db.entities.Setup;
import space.npstr.wolfia.game.definitions.Games;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by npstr on 23.08.2016
 */
@Component
public class OutCommand implements BaseCommand {

    public static final String TRIGGER = "out";

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("leave");
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
                final Setup s = Launcher.getBotContext().getDatabase().getWrapper().findApplyAndMerge(Setup.key(context.textChannel.getIdLong()),
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
            if (Launcher.getBotContext().getDatabase().getWrapper().getOrCreate(Setup.key(context.textChannel.getIdLong())).isInned(context.invoker.getIdLong())) {
                //handling a regular out
                Launcher.getBotContext().getDatabase().getWrapper().findApplyAndMerge(Setup.key(context.textChannel.getIdLong()),
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
