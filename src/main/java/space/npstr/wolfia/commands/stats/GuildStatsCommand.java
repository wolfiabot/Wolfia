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

import net.dv8tion.jda.core.entities.Guild;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;

/**
 * Created by napster on 10.06.17.
 * <p>
 * Display stats for a guild
 */
public class GuildStatsCommand extends BaseCommand {

    public GuildStatsCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " [guild ID]"
                + "\n#Show game stats for this guild or another one. Examples:"
                + "\n  " + invocation()
                + "\n  " + invocation() + " 315944983754571796";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context)
            throws IllegalGameStateException, DatabaseException {

        if (context.hasArguments()) {
            try {
                final long guildId = Long.parseLong(context.args[0]);
                context.reply("This command has been temporarily disabled. It may or may not come back in the future.");
//                context.reply(StatsProvider.getGuildStats(guildId).build());  TODO enable again after StatsProvider has been rewritten
                return true;
            } catch (final NumberFormatException e) {
                context.help();
                return false;
            }
        }

        final Guild guild = context.getGuild();
        if (guild == null) {
            context.help();
            return false;
        }
        
        final long guildId = context.getGuild().getIdLong();
        context.reply("This command has been temporarily disabled. It may or may not come back in the future.");
//        context.reply(StatsProvider.getGuildStats(guildId).build());  TODO enable again after StatsProvider has been rewritten
        return true;
    }
}
