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

import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.StatsProvider;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by napster on 10.06.17.
 * <p>
 * Display stats for a guild
 */
public class GuildStatsCommand extends BaseCommand {

    public static final String COMMAND = "guildstats";

    @Override
    public String help() {
        return Config.PREFIX + COMMAND + " [guild ID]"
                + "\n#Show game stats for this guild or another one. Examples:"
                + "\n  " + Config.PREFIX + COMMAND
                + "\n  " + Config.PREFIX + COMMAND + " 315944983754571796";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        long guildId = commandInfo.event.getGuild().getIdLong();

        //noinspection Duplicates
        if (commandInfo.args.length > 0) {
            try {
                guildId = Long.valueOf(commandInfo.args[0]);
            } catch (final NumberFormatException e) {
                Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(), "%s", TextchatUtils.asMarkdown(help()));
                return false;
            }
        }
        Wolfia.handleOutputEmbed(commandInfo.event.getTextChannel(), StatsProvider.getGuildStats(guildId).build());
        return true;
    }
}
