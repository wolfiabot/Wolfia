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

package space.npstr.wolfia.domain.stats;

import net.dv8tion.jda.core.entities.User;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;

import javax.annotation.Nonnull;

/**
 * Created by napster on 08.06.17.
 * <p>
 * Display stats for a user
 */
@Command
public class UserStatsCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "userstats";

    private final StatsProvider statsProvider;
    private final StatsRender render;

    public UserStatsCommand(StatsProvider statsProvider, StatsRender render) {
        this.statsProvider = statsProvider;
        this.render = render;
    }

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
    public boolean execute(@Nonnull final CommandContext context) {
        if (context.msg.getMentionedUsers().isEmpty()) {
            long userId = context.invoker.getIdLong();
            //noinspection Duplicates
            if (context.hasArguments()) {
                try {
                    userId = Long.parseLong(context.args[0]);
                } catch (final NumberFormatException e) {
                    context.help();
                    return false;
                }
            }

            UserStats userStats = this.statsProvider.getUserStats(userId);
            context.reply(this.render.renderUserStats(userStats).build());
            return true;
        }

        for (final User user : context.msg.getMentionedUsers()) {
            UserStats userStats = this.statsProvider.getUserStats(user.getIdLong());
            context.reply(this.render.renderUserStats(userStats).build());
        }
        return true;
    }


}
