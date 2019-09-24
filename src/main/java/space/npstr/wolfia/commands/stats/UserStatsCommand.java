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

package space.npstr.wolfia.commands.stats;

import net.dv8tion.jda.core.entities.User;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.StatsProvider;

import javax.annotation.Nonnull;

/**
 * Created by napster on 08.06.17.
 * <p>
 * Display stats for a user
 */
@Component
public class UserStatsCommand implements BaseCommand {

    public static final String TRIGGER = "userstats";

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " [@user or user ID]"
                + "\n#Show game stats for yourself or another a user. Examples:"
                + "\n  " + invocation()
                + "\n  " + invocation() + " @Napster"
                + "\n  " + invocation() + " 166604053629894657";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context)
            throws IllegalGameStateException, DatabaseException {

        long userId = context.invoker.getIdLong();
        if (context.msg.getMentionedUsers().isEmpty()) {
            //noinspection Duplicates
            if (context.hasArguments()) {
                try {
                    userId = Long.parseLong(context.args[0]);
                } catch (final NumberFormatException e) {
                    context.help();
                    return false;
                }
            }

            context.reply(StatsProvider.getUserStats(userId).build());
            return true;
        }

        for (final User u : context.msg.getMentionedUsers()) {
            context.reply(StatsProvider.getUserStats(u.getIdLong()).build());
        }
        return true;
    }


}
