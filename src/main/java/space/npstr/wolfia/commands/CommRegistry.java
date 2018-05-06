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

package space.npstr.wolfia.commands;

import lombok.extern.slf4j.Slf4j;
import space.npstr.wolfia.commands.debug.BanCommand;
import space.npstr.wolfia.commands.debug.DbTestCommand;
import space.npstr.wolfia.commands.debug.EvalCommand;
import space.npstr.wolfia.commands.debug.KillGameCommand;
import space.npstr.wolfia.commands.debug.MaintenanceCommand;
import space.npstr.wolfia.commands.debug.RegisterPrivateServerCommand;
import space.npstr.wolfia.commands.debug.RestartCommand;
import space.npstr.wolfia.commands.debug.RunningCommand;
import space.npstr.wolfia.commands.debug.ShutdownCommand;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by napster on 07.12.17.
 */
@Slf4j
public class CommRegistry {

    //holder pattern
    private static class CommRegistryHolder {
        private final static CommRegistry INSTANCE = new CommRegistry();
    }

    public static CommRegistry getRegistry() {
        return CommRegistryHolder.INSTANCE;
    }


    private final List<BaseCommand> commands = new ArrayList<>();

    public int size() {
        return this.commands.size();
    }

    @Nullable
    public BaseCommand getCommand(@Nonnull final String input) {
        return this.commands.stream()
                .filter(command -> input.equalsIgnoreCase(command.name) || command.aliases.contains(input.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    public static final String COMM_TRIGGER_IN = "in";
    public static final String COMM_TRIGGER_OUT = "out";
    public static final String COMM_TRIGGER_ROLEPM = "rolepm";
    public static final String COMM_TRIGGER_SETUP = "setup";
    public static final String COMM_TRIGGER_START = "start";
    public static final String COMM_TRIGGER_STATUS = "status";

    public static final String COMM_TRIGGER_SHOOT = "shoot";
    public static final String COMM_TRIGGER_VOTE = "vote";
    public static final String COMM_TRIGGER_UNVOTE = "unvote";
    public static final String COMM_TRIGGER_CHECK = "check";
    public static final String COMM_TRIGGER_VOTECOUNT = "votecount";
    public static final String COMM_TRIGGER_NIGHTKILL = "nightkill";
    public static final String COMM_TRIGGER_HOHOHO = "hohoho";
    public static final String COMM_TRIGGER_OPENPRESENT = "openpresent";
    public static final String COMM_TRIGGER_OPENPRESENT_ALIAS = "op";
    public static final String COMM_TRIGGER_ITEMS = "items";

    public static final String COMM_TRIGGER_BOTSTATS = "botstats";
    public static final String COMM_TRIGGER_GUILDSTATS = "guildstats";
    public static final String COMM_TRIGGER_USERSTATS = "userstats";

    public static final String COMM_TRIGGER_CHANNELSETTINGS = "channelsettings";
    public static final String COMM_TRIGGER_COMMANDS = "commands";
    public static final String COMM_TRIGGER_HELP = "help";
    public static final String COMM_TRIGGER_INFO = "info";
    public static final String COMM_TRIGGER_INVITE = "invite";
    public static final String COMM_TRIGGER_RANK = "rank";
    public static final String COMM_TRIGGER_REPLAY = "replay";
    public static final String COMM_TRIGGER_TAG = "tag";


    private CommRegistry() {
        //@formatter:off

        //game related commands
        registerCommand(new InCommand                        (COMM_TRIGGER_IN, "join"));
        registerCommand(new OutCommand                       (COMM_TRIGGER_OUT, "leave"));
        registerCommand(new RolePmCommand                    (COMM_TRIGGER_ROLEPM, "rpm"));
        registerCommand(new SetupCommand                     (COMM_TRIGGER_SETUP));
        registerCommand(new StartCommand                     (COMM_TRIGGER_START));
        registerCommand(new StatusCommand                    (COMM_TRIGGER_STATUS, "st"));

        //ingame commands
        registerCommand(new ShootCommand                     (COMM_TRIGGER_SHOOT, "s", "blast"));
        registerCommand(new VoteCommand                      (COMM_TRIGGER_VOTE, "v", "lynch"));
        registerCommand(new UnvoteCommand                    (COMM_TRIGGER_UNVOTE, "u", "uv"));
        registerCommand(new CheckCommand                     (COMM_TRIGGER_CHECK));
        registerCommand(new VoteCountCommand                 (COMM_TRIGGER_VOTECOUNT, "vc"));
        registerCommand(new NightkillCommand                 (COMM_TRIGGER_NIGHTKILL, "nk"));
        registerCommand(new HohohoCommand                    (COMM_TRIGGER_HOHOHO, "ho"));
        registerCommand(new OpenPresentCommand               (COMM_TRIGGER_OPENPRESENT, "open", COMM_TRIGGER_OPENPRESENT_ALIAS));
        registerCommand(new ItemsCommand                     (COMM_TRIGGER_ITEMS));

        //stats commands
        registerCommand(new BotStatsCommand                  (COMM_TRIGGER_BOTSTATS));
        registerCommand(new GuildStatsCommand                (COMM_TRIGGER_GUILDSTATS));
        registerCommand(new UserStatsCommand                 (COMM_TRIGGER_USERSTATS));

        //util commands
        registerCommand(new ChannelSettingsCommand           (COMM_TRIGGER_CHANNELSETTINGS, "cs"));
        registerCommand(new CommandsCommand                  (COMM_TRIGGER_COMMANDS, "comms"));
        registerCommand(new HelpCommand                      (COMM_TRIGGER_HELP));
        registerCommand(new InfoCommand                      (COMM_TRIGGER_INFO));
        registerCommand(new InviteCommand                    (COMM_TRIGGER_INVITE, "inv"));
        registerCommand(new RankCommand                      (COMM_TRIGGER_RANK));
        registerCommand(new ReplayCommand                    (COMM_TRIGGER_REPLAY));
        registerCommand(new TagCommand                       (COMM_TRIGGER_TAG));

        //bot owner/debug commands
        registerCommand(new BanCommand                       ("ban"));
        registerCommand(new DbTestCommand                    ("dbtest"));
        registerCommand(new EvalCommand                      ("eval"));
        registerCommand(new KillGameCommand                  ("killgame"));
        registerCommand(new MaintenanceCommand               ("maint"));
        registerCommand(new RegisterPrivateServerCommand     ("register"));
        registerCommand(new RestartCommand                   ("restart"));
        registerCommand(new RunningCommand                   ("running"));
        registerCommand(new ShutdownCommand                  ("shutdown"));
        registerCommand(new SyncCommand                      ("sync"));

        //@formatter:on
    }

    private void registerCommand(final BaseCommand command) {
        for (final String trigger : command.aliases) {
            if (getCommand(trigger) != null) {
                log.error("Duplicate command trigger: {}", trigger);
            }
        }
        this.commands.add(command);
    }
}
