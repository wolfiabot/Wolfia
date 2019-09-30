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

import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.StatsProvider;

import javax.annotation.Nonnull;

/**
 * Created by napster on 10.06.17.
 * <p>
 * Show stats of the whole bot
 */
@Component
public class BotStatsCommand implements BaseCommand {

    public static final String TRIGGER = "botstats";

    private final StatsProvider statsProvider;

    public BotStatsCommand(StatsProvider statsProvider) {
        this.statsProvider = statsProvider;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Nonnull
    @Override
    public String help() {
        return invocation()
                + "\n#Show game stats for all games ever played with Wolfia.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context)
            throws DatabaseException, IllegalGameStateException {
        context.reply(this.statsProvider.getBotStats(context).build());
        return true;
    }
}
