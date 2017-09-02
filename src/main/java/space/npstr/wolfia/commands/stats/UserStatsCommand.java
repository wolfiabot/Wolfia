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

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.StatsProvider;

/**
 * Created by napster on 08.06.17.
 * <p>
 * Display stats for a user
 */
public class UserStatsCommand extends BaseCommand {

    public UserStatsCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger() + " [@user or user ID]"
                + "\n#Show game stats for yourself or another a user. Examples:"
                + "\n  " + Config.PREFIX + getMainTrigger()
                + "\n  " + Config.PREFIX + getMainTrigger() + " @Napster"
                + "\n  " + Config.PREFIX + getMainTrigger() + " 166604053629894657";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        final Message m = commandInfo.event.getMessage();
        long userId = m.getAuthor().getIdLong();

        if (m.getMentionedUsers().size() < 1) {
            //noinspection Duplicates
            if (commandInfo.args.length > 0) {
                try {
                    userId = Long.parseLong(commandInfo.args[0]);
                } catch (final NumberFormatException e) {
                    commandInfo.reply(formatHelp(commandInfo.invoker));
                    return false;
                }
            }

            Wolfia.handleOutputEmbed(m.getChannel(), StatsProvider.getUserStats(userId).build());
            return true;
        }

        for (final User u : m.getMentionedUsers()) {
            Wolfia.handleOutputEmbed(m.getChannel(), StatsProvider.getUserStats(u.getIdLong()).build());
        }
        return true;
    }


}
