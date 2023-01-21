/*
 * Copyright (C) 2016-2020 the original author or authors
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

import java.util.Optional;
import org.springframework.lang.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;

/**
 * Display stats for a guild
 */
@Command
public class GuildStatsCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "guildstats";

    private final StatsProvider statsProvider;
    private final StatsRender render;

    public GuildStatsCommand(StatsProvider statsProvider, StatsRender render) {
        this.statsProvider = statsProvider;
        this.render = render;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @NonNull
    @Override
    public String help() {
        return invocation() + " [guild ID]"
                + "\n#Show game stats for this guild or another one. Examples:"
                + "\n  " + invocation()
                + "\n  " + invocation() + " 315944983754571796";
    }

    @Override
    public boolean execute(@NonNull final CommandContext context) {
        Optional<Long> guildId = Optional.empty();
        if (context.hasArguments()) {
            try {
                guildId = Optional.of(Long.parseLong(context.args[0]));
            } catch (final NumberFormatException e) {
                context.help();
                return false;
            }
        }

        if (guildId.isEmpty()) {
            Optional<Guild> guild = context.getGuild();
            if (guild.isEmpty()) {
                context.help();
                return false;
            }
            guildId = guild.map(Guild::getIdLong);
        }

        GuildStats guildStats = this.statsProvider.getGuildStats(guildId.get());
        context.reply(this.render.renderGuildStats(context, guildStats).build());
        return true;
    }
}
