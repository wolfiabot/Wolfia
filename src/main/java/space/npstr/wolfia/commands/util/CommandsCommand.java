package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.EmbedBuilder;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.game.InCommand;
import space.npstr.wolfia.commands.game.OutCommand;
import space.npstr.wolfia.commands.game.RolePmCommand;
import space.npstr.wolfia.commands.game.SetupCommand;
import space.npstr.wolfia.commands.game.StartCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.commands.ingame.CheckCommand;
import space.npstr.wolfia.commands.ingame.NightkillCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.commands.ingame.UnvoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.commands.stats.BotStatsCommand;
import space.npstr.wolfia.commands.stats.GuildStatsCommand;
import space.npstr.wolfia.commands.stats.UserStatsCommand;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

public class CommandsCommand extends BaseCommand {

    public static final String COMMAND = "commands";

    @Override
    public String help() {
        return Config.PREFIX + COMMAND
                + "\n#Show all available commands.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        //@formatter:off
        final String gameCommands = ""
                + Config.PREFIX + InCommand.COMMAND + "\n"
                + Config.PREFIX + OutCommand.COMMAND + "\n"
                + Config.PREFIX + SetupCommand.COMMAND + "\n"
                + Config.PREFIX + StartCommand.COMMAND + "\n"
                + Config.PREFIX + RolePmCommand.COMMAND + "\n"
                + Config.PREFIX + StatusCommand.COMMAND + "\n"
                ;

        final String ingameCommands = ""
                + Config.PREFIX + CheckCommand.COMMAND + "\n"
                + Config.PREFIX + ShootCommand.COMMAND + "\n"
                + Config.PREFIX + UnvoteCommand.COMMAND + "\n"
                + Config.PREFIX + VoteCommand.COMMAND + "\n"
                + Config.PREFIX + VoteCountCommand.COMMAND + "\n"
                + Config.PREFIX + NightkillCommand.COMMAND + "\n"
                ;

        final String settingsCommands =
                Config.PREFIX + ChannelSettingsCommand.COMMAND;

        final String statsCommands = ""
                + Config.PREFIX + UserStatsCommand.COMMAND + "\n"
                + Config.PREFIX + GuildStatsCommand.COMMAND + "\n"
                + Config.PREFIX + BotStatsCommand.COMMAND + "\n"
                ;

        final String otherCommands = ""
                + Config.PREFIX + CommandsCommand.COMMAND + "\n"
                + Config.PREFIX + HelpCommand.COMMAND + "\n"
                + Config.PREFIX + InfoCommand.COMMAND + "\n"
                + Config.PREFIX + ReplayCommand.COMMAND + "\n"
                + Config.PREFIX + TagCommand.COMMAND + "\n"
                ;
        //@formatter:on

        final EmbedBuilder eb = new EmbedBuilder();
        final String link = App.DOCS_LINK + "#commands";
        eb.setTitle("Wolfia commands", link);
        eb.addField("Starting a game", gameCommands, true);
        eb.addField("Game actions", ingameCommands, true);
        eb.addField("Settings", settingsCommands, true);
        eb.addField("Statistics", statsCommands, true);
        eb.addField("Other Commands", otherCommands, true);
        eb.addBlankField(true);

        eb.addField("", "**Head over to** " + link + " **for the full commands reference**", false);

        commandInfo.reply(eb.build());
        return true;
    }
}
