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

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.StatsProvider;

/**
 * Created by napster on 08.06.17.
 * <p>
 * Display stats for a user
 */
public class UserStatsCommand implements ICommand {

    public static final String COMMAND = "userstats";


    @Override
    public String help() {
        return "Shows game stats for a user";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        final Message m = commandInfo.event.getMessage();
        if (m.getMentionedUsers().size() < 1) {
            Wolfia.handleOutputEmbed(m.getChannel(), StatsProvider.getUserStats(commandInfo.event.getMember()).build());
            return true;
        }

        for (final User u : m.getMentionedUsers()) {
            if (!m.getGuild().isMember(u)) continue; //skip mentioned non-members
            Wolfia.handleOutputEmbed(m.getChannel(), StatsProvider.getUserStats(m.getGuild().getMember(u)).build());
        }
        return true;
    }


}
