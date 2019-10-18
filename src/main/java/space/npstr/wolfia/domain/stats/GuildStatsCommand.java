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

import net.dv8tion.jda.core.entities.Guild;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.StatsProvider;

import javax.annotation.Nonnull;

/**
 * Created by napster on 10.06.17.
 * <p>
 * Display stats for a guild
 */
@Command
public class GuildStatsCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "guildstats";

    private final StatsProvider statsProvider;

    public GuildStatsCommand(StatsProvider statsProvider) {
        this.statsProvider = statsProvider;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
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
            throws IllegalGameStateException {

        if (context.hasArguments()) {
            try {
                final long guildId = Long.parseLong(context.args[0]);
                context.reply(this.statsProvider.getGuildStats(context, guildId).build());
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
        context.reply(this.statsProvider.getGuildStats(context, guildId).build());
        return true;
    }
}
