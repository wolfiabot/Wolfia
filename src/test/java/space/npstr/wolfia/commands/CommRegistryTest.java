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

package space.npstr.wolfia.commands;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.commands.debug.EvalCommand;
import space.npstr.wolfia.commands.debug.KillGameCommand;
import space.npstr.wolfia.commands.debug.MaintenanceCommand;
import space.npstr.wolfia.commands.debug.RegisterPrivateServerCommand;
import space.npstr.wolfia.commands.debug.RestartCommand;
import space.npstr.wolfia.commands.debug.ReviveCommand;
import space.npstr.wolfia.commands.debug.RunningCommand;
import space.npstr.wolfia.commands.debug.SyncCommand;
import space.npstr.wolfia.commands.game.InCommand;
import space.npstr.wolfia.commands.game.OutCommand;
import space.npstr.wolfia.commands.game.RolePmCommand;
import space.npstr.wolfia.commands.game.SetupCommand;
import space.npstr.wolfia.commands.game.StartCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.commands.ingame.CheckCommand;
import space.npstr.wolfia.commands.ingame.HohohoCommand;
import space.npstr.wolfia.commands.ingame.ItemsCommand;
import space.npstr.wolfia.commands.ingame.NightkillCommand;
import space.npstr.wolfia.commands.ingame.OpenPresentCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.commands.ingame.UnvoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.commands.stats.BotStatsCommand;
import space.npstr.wolfia.commands.stats.GuildStatsCommand;
import space.npstr.wolfia.commands.stats.UserStatsCommand;
import space.npstr.wolfia.commands.util.ChannelSettingsCommand;
import space.npstr.wolfia.commands.util.CommandsCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.commands.util.InfoCommand;
import space.npstr.wolfia.commands.util.InviteCommand;
import space.npstr.wolfia.commands.util.RankCommand;
import space.npstr.wolfia.commands.util.ReplayCommand;
import space.npstr.wolfia.commands.util.TagCommand;
import space.npstr.wolfia.domain.ban.BanCommand;

import static org.assertj.core.api.Assertions.assertThat;

class CommRegistryTest extends ApplicationTest {

    @Autowired
    private CommRegistry commRegistry;

    // Game commands

    @Test
    void hasInCommand() {
        assertHasCommand("in", InCommand.class);
        assertHasCommand("join", InCommand.class);
    }

    @Test
    void hasOutCommand() {
        assertHasCommand("out", OutCommand.class);
        assertHasCommand("leave", OutCommand.class);
    }

    @Test
    void hasRolePmCommand() {
        assertHasCommand("rolepm", RolePmCommand.class);
        assertHasCommand("rpm", RolePmCommand.class);
    }

    @Test
    void hasSetupCommand() {
        assertHasCommand("setup", SetupCommand.class);
    }

    @Test
    void hasStartCommand() {
        assertHasCommand("start", StartCommand.class);
    }

    @Test
    void hasStatusCommand() {
        assertHasCommand("status", StatusCommand.class);
        assertHasCommand("st", StatusCommand.class);
    }


    // Ingame commands

    //ShootCommand
    //VoteCommand
    //UnvoteCommand
    //CheckCommand
    //VoteCountCommand
    //NightkillCommand
    //HohohoCommand
    //OpenPresentCommand
    //ItemsCommand
    @Test
    void hasShootCommand() {
        assertHasCommand("shoot", ShootCommand.class);
        assertHasCommand("s", ShootCommand.class);
        assertHasCommand("blast", ShootCommand.class);

    }

    @Test
    void hasVoteCommand() {
        assertHasCommand("vote", VoteCommand.class);
        assertHasCommand("v", VoteCommand.class);
        assertHasCommand("lynch", VoteCommand.class);

    }

    @Test
    void hasUnvoteCommand() {
        assertHasCommand("unvote", UnvoteCommand.class);
        assertHasCommand("u", UnvoteCommand.class);
        assertHasCommand("uv", UnvoteCommand.class);
    }

    @Test
    void hasCheckCommand() {
        assertHasCommand("check", CheckCommand.class);
    }

    @Test
    void hasVoteCountCommand() {
        assertHasCommand("votecount", VoteCountCommand.class);
        assertHasCommand("vc", VoteCountCommand.class);
    }

    @Test
    void hasNightkillCommand() {
        assertHasCommand("nightkill", NightkillCommand.class);
        assertHasCommand("nk", NightkillCommand.class);
    }

    @Test
    void hasHohohoCommand() {
        assertHasCommand("hohoho", HohohoCommand.class);
        assertHasCommand("ho", HohohoCommand.class);
    }

    @Test
    void hasOpenPresentCommand() {
        assertHasCommand("openpresent", OpenPresentCommand.class);
        assertHasCommand("open", OpenPresentCommand.class);
        assertHasCommand("op", OpenPresentCommand.class);
    }

    @Test
    void hasItemsCommand() {
        assertHasCommand("items", ItemsCommand.class);
    }


    // Stats commands

    @Test
    void hasBotStatsCommand() {
        assertHasCommand("botstats", BotStatsCommand.class);
    }

    @Test
    void hasGuildStatsCommand() {
        assertHasCommand("guildstats", GuildStatsCommand.class);
    }

    @Test
    void hasUserStatsCommand() {
        assertHasCommand("userstats", UserStatsCommand.class);
    }


    // Util commands

    @Test
    void hasChannelSettingsCommand() {
        assertHasCommand("channelsettings", ChannelSettingsCommand.class);
        assertHasCommand("cs", ChannelSettingsCommand.class);
    }

    @Test
    void hasCommandsCommand() {
        assertHasCommand("commands", CommandsCommand.class);
        assertHasCommand("comms", CommandsCommand.class);
    }

    @Test
    void hasHelpCommand() {
        assertHasCommand("help", HelpCommand.class);
    }

    @Test
    void hasInfoCommand() {
        assertHasCommand("info", InfoCommand.class);
    }

    @Test
    void hasInviteCommand() {
        assertHasCommand("invite", InviteCommand.class);
        assertHasCommand("inv", InviteCommand.class);
    }

    @Test
    void hasRankCommand() {
        assertHasCommand("rank", RankCommand.class);
    }

    @Test
    void hasReplayCommand() {
        assertHasCommand("replay", ReplayCommand.class);
    }

    @Test
    void hasTagCommand() {
        assertHasCommand("tag", TagCommand.class);
    }


    // Administrative commands

    @Test
    void hasBanCommand() {
        assertHasCommand("ban", BanCommand.class);
    }

    @Test
    void hasEvalCommand() {
        assertHasCommand("eval", EvalCommand.class);
    }

    @Test
    void hasKillGameCommand() {
        assertHasCommand("killgame", KillGameCommand.class);
    }

    @Test
    void hasMaintenanceCommand() {
        assertHasCommand("maint", MaintenanceCommand.class);
    }

    @Test
    void hasRegisterPrivateServerCommand() {
        assertHasCommand("register", RegisterPrivateServerCommand.class);
    }

    @Test
    void hasRestartCommand() {
        assertHasCommand("restart", RestartCommand.class);
    }

    @Test
    void hasReviveCommand() {
        assertHasCommand("revive", ReviveCommand.class);
    }

    @Test
    void hasRunningCommand() {
        assertHasCommand("running", RunningCommand.class);
    }

    @Test
    void hasSyncCommand() {
        assertHasCommand("sync", SyncCommand.class);
    }

    private void assertHasCommand(String trigger, Class<? extends BaseCommand> clazz) {
        BaseCommand command = commRegistry.getCommand(trigger);

        assertThat(command)
                .as("Command %s not found for trigger %s", clazz.getSimpleName(), trigger)
                .isNotNull();

        assertThat(command)
                .as("Command %s found for trigger %s, but was expecting %s",
                        command.getClass().getSimpleName(), trigger, clazz.getSimpleName())
                .isInstanceOf(clazz);
    }

}
