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

import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.db.entities.Banlist;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.db.entities.SetupEntity;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Scope;

import javax.annotation.Nonnull;

/**
 * Created by npstr on 23.08.2016
 */
public class InCommand extends BaseCommand {

//    private final int MAX_SIGNUP_TIME = 10 * 60; //10h

    public InCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + "\n#Add you to the signup list for this channel. You will play in the next starting game.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws DatabaseException {

//        long timeForSignup = Long.valueOf(args[0]);
//        timeForSignup = timeForSignup < this.MAX_SIGNUP_TIME ? timeForSignup : this.MAX_SIGNUP_TIME;

        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        //is there a game going on?
        if (Games.get(context.textChannel) != null) {
            context.replyWithMention("the game has already started! Please wait until it is over to join.");
            return false;
        }

        //check for private guilds where we dont want games to be started
        if (PrivateGuild.isPrivateGuild(context.guild)) {
            context.replyWithMention("you can't play games in a private guild.");
            return false;
        }

        final SetupEntity setup = SetupEntity.load(context.textChannel.getIdLong());

        //force in by bot owner ( ͡° ͜ʖ ͡°)
        if (!context.msg.getMentionedUsers().isEmpty() && context.isOwner()) {
            for (final User u : context.msg.getMentionedUsers()) {
                setup.inUser(u.getIdLong(), context);
            }
            context.reply(setup.getStatus());
            return true;
        }

        if (Banlist.load(context.invoker.getIdLong()).getScope() == Scope.GLOBAL) {
            context.replyWithMention("lol ur banned.");
            return false;
        }

        if (setup.inUser(context.invoker.getIdLong(), context)) {
            context.reply(setup.getStatus());
            return true;
        } else {
            return false;
        }
    }
}
