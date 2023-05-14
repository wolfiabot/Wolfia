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
 * Shows replays of games that are over
 */
@Command
public class ReplayCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "replay";

    private final StatsRender render;
    private final StatsProvider statsProvider;

    public ReplayCommand(StatsRender render, StatsProvider statsProvider) {
        this.render = render;
        this.statsProvider = statsProvider;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public String help() {
        return invocation() + " #gameid"
                + "\n#Show the replay of a game. Examples:"
                + "\n  " + invocation() + " #69"
                + "\n  " + invocation() + " 9001";
    }

    @Override
    public boolean execute(CommandContext context) {

        if (!context.hasArguments()) {
            context.help();
            return false;
        }

        long gameId;
        try {
            gameId = Long.parseLong(context.args[0].replace("#", ""));
        } catch (NumberFormatException ex) {
            context.help();
            return false;
        }

        GameStats gameStats = this.statsProvider.calculateGameStats(gameId);

        if (gameStats == null) {
            context.replyWithMention("there is no such game in the database.");
            return false;
        }

        context.reply(this.render.renderGameStats(gameStats));
        return true;
    }
}
