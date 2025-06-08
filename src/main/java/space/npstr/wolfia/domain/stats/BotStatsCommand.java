/*
 * Copyright (C) 2016-2023 the original author or authors
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

import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;

/**
 * Show stats of the whole bot
 */
@Command
public class BotStatsCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "botstats";

    private final StatsProvider statsProvider;
    private final StatsRender render;

    public BotStatsCommand(StatsProvider statsProvider, StatsRender render) {
        this.statsProvider = statsProvider;
        this.render = render;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public String help() {
        return invocation()
                + "\n#Show game stats for all games ever played with Wolfia.";
    }

    @Override
    public boolean execute(CommandContext context) {
        BotStats botStats = this.statsProvider.calculateBotStats();
        context.reply(this.render.renderBotStats(context, botStats));
        return true;
    }
}
