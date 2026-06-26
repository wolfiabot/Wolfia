/*
 * Copyright (C) 2016-2026 the original author or authors
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

package space.npstr.wolfia.commands;

import java.util.List;
import java.util.Set;
import space.npstr.wolfia.commands.game.RolePmCommand;
import space.npstr.wolfia.commands.game.StartCommand;
import space.npstr.wolfia.commands.ingame.CheckCommand;
import space.npstr.wolfia.commands.ingame.HohohoCommand;
import space.npstr.wolfia.commands.ingame.ItemsCommand;
import space.npstr.wolfia.commands.ingame.NightkillCommand;
import space.npstr.wolfia.commands.ingame.OpenPresentCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.commands.ingame.UnvoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.commands.util.CommandsCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.commands.util.InfoCommand;
import space.npstr.wolfia.commands.util.InviteCommand;
import space.npstr.wolfia.commands.util.RankCommand;
import space.npstr.wolfia.commands.util.TagCommand;
import space.npstr.wolfia.domain.oauth2.AuthCommand;
import space.npstr.wolfia.domain.privacy.PrivacyCommand;
import space.npstr.wolfia.domain.settings.ChannelSettingsCommand;
import space.npstr.wolfia.domain.setup.InCommand;
import space.npstr.wolfia.domain.setup.OutCommand;
import space.npstr.wolfia.domain.setup.SetupCommand;
import space.npstr.wolfia.domain.setup.StatusCommand;
import space.npstr.wolfia.domain.stats.BotStatsCommand;
import space.npstr.wolfia.domain.stats.GuildStatsCommand;
import space.npstr.wolfia.domain.stats.ReplayCommand;
import space.npstr.wolfia.domain.stats.UserStatsCommand;

/**
 * The user facing grouping of public commands.
 *
 * <p>This is the single source of truth shared by the in-Discord {@code w.commands} output
 * ({@link CommandsCommand}) and the public web API ({@code /public/commands}), so both stay in sync.
 */
public enum CommandCategory {

    STARTING_A_GAME("Starting a game", List.of(
            InCommand.TRIGGER,
            OutCommand.TRIGGER,
            SetupCommand.TRIGGER,
            StartCommand.TRIGGER,
            RolePmCommand.TRIGGER,
            StatusCommand.TRIGGER
    )),
    GAME_ACTIONS("Game actions", List.of(
            ShootCommand.TRIGGER,
            VoteCommand.TRIGGER,
            UnvoteCommand.TRIGGER,
            VoteCountCommand.TRIGGER,
            NightkillCommand.TRIGGER,
            CheckCommand.TRIGGER,
            HohohoCommand.TRIGGER,
            ItemsCommand.TRIGGER,
            OpenPresentCommand.TRIGGER
    )),
    SETTINGS("Settings", List.of(
            ChannelSettingsCommand.TRIGGER
    )),
    STATISTICS("Statistics", List.of(
            UserStatsCommand.TRIGGER,
            GuildStatsCommand.TRIGGER,
            BotStatsCommand.TRIGGER
    )),
    OTHER("Other Commands", List.of(
            AuthCommand.TRIGGER,
            CommandsCommand.TRIGGER,
            HelpCommand.TRIGGER,
            InfoCommand.TRIGGER,
            InviteCommand.TRIGGER,
            PrivacyCommand.TRIGGER,
            RankCommand.TRIGGER,
            ReplayCommand.TRIGGER,
            TagCommand.TRIGGER
    )),
    ;

    /**
     * Triggers that are only usable while a game in the seasonal Xmas mode is running.
     */
    public static final Set<String> XMAS_ONLY_TRIGGERS = Set.of(
            HohohoCommand.TRIGGER,
            ItemsCommand.TRIGGER,
            OpenPresentCommand.TRIGGER
    );

    private final String displayName;
    private final List<String> triggers;

    CommandCategory(String displayName, List<String> triggers) {
        this.displayName = displayName;
        this.triggers = triggers;
    }

    public String displayName() {
        return this.displayName;
    }

    public List<String> triggers() {
        return this.triggers;
    }

    public static boolean isXmasOnly(String trigger) {
        return XMAS_ONLY_TRIGGERS.contains(trigger);
    }
}
