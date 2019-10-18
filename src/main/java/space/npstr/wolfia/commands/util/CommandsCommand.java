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

package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.EmbedBuilder;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.commands.PublicCommand;
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
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.settings.ChannelSettingsCommand;
import space.npstr.wolfia.domain.setup.InCommand;
import space.npstr.wolfia.domain.setup.OutCommand;
import space.npstr.wolfia.domain.setup.SetupCommand;
import space.npstr.wolfia.domain.setup.StatusCommand;
import space.npstr.wolfia.domain.stats.BotStatsCommand;
import space.npstr.wolfia.domain.stats.GuildStatsCommand;
import space.npstr.wolfia.domain.stats.UserStatsCommand;

import javax.annotation.Nonnull;
import java.util.List;

@Command
public class CommandsCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "commands";

    private static final String XMAS_MODE_ONLY = " _(xmas mode only)_";

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("comms");
    }

    @Nonnull
    @Override
    public String help() {
        return invocation()
                + "\n#Show all available commands.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) {
        //@formatter:off
        final String gameCommands = ""
                + WolfiaConfig.DEFAULT_PREFIX + InCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + OutCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + SetupCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + StartCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + RolePmCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + StatusCommand.TRIGGER + "\n"
                ;

        final String ingameCommands = ""
                + WolfiaConfig.DEFAULT_PREFIX + ShootCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + VoteCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + UnvoteCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + VoteCountCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + NightkillCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CheckCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + HohohoCommand.TRIGGER + XMAS_MODE_ONLY + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER + XMAS_MODE_ONLY + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + OpenPresentCommand.TRIGGER + XMAS_MODE_ONLY + "\n"
                ;

        final String settingsCommands =
                  WolfiaConfig.DEFAULT_PREFIX + ChannelSettingsCommand.TRIGGER
                ;

        final String statsCommands = ""
                + WolfiaConfig.DEFAULT_PREFIX + UserStatsCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + GuildStatsCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + BotStatsCommand.TRIGGER + "\n"
                ;

        final String otherCommands = ""
                + WolfiaConfig.DEFAULT_PREFIX + CommandsCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + InfoCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + InviteCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + RankCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + ReplayCommand.TRIGGER + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + TagCommand.TRIGGER + "\n"
                ;
        //@formatter:on


        final String link = App.DOCS_LINK + "#commands";
        final EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder()
                .setTitle("Wolfia commands", link)
                .addField("Starting a game", gameCommands, true)
                .addField("Game actions", ingameCommands, true)
                .addField("Settings", settingsCommands, true)
                .addField("Statistics", statsCommands, true)
                .addField("Other Commands", otherCommands, true)
                .addBlankField(true)
                .addField("", "**Head over to** " + link + " **for the full commands reference" +
                        " or run **`w.help [command]`** for detailed information on a command.**", false);

        context.reply(eb.build());
        return true;
    }
}
