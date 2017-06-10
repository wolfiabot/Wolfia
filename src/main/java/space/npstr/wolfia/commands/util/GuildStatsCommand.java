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

import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.StatsProvider;

/**
 * Created by napster on 10.06.17.
 * <p>
 * Display stats for a guild
 */
public class GuildStatsCommand implements ICommand {

    public static final String COMMAND = "guildstats";

    @Override
    public String help() {
        return "todo";//todo
    }

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        Wolfia.handleOutputEmbed(commandInfo.event.getTextChannel(), StatsProvider.getGuildStats(commandInfo.event.getGuild()).build());
    }
}
