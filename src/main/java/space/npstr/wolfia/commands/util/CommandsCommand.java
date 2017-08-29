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

import static space.npstr.wolfia.commands.CommandHandler.mainTrigger;

public class CommandsCommand extends BaseCommand {

    public CommandsCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger()
                + "\n#Show all available commands.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        //@formatter:off
        final String gameCommands = ""
                + Config.PREFIX + mainTrigger(InCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(OutCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(SetupCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(StartCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(RolePmCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(StatusCommand.class) + "\n"
                ;

        final String ingameCommands = ""
                + Config.PREFIX + mainTrigger(CheckCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(ShootCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(UnvoteCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(VoteCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(VoteCountCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(NightkillCommand.class) + "\n"
                ;

        final String settingsCommands =
                  Config.PREFIX + mainTrigger(ChannelSettingsCommand.class)
                ;

        final String statsCommands = ""
                + Config.PREFIX + mainTrigger(UserStatsCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(GuildStatsCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(BotStatsCommand.class) + "\n"
                ;

        final String otherCommands = ""
                + Config.PREFIX + mainTrigger(CommandsCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(HelpCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(InfoCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(ReplayCommand.class) + "\n"
                + Config.PREFIX + mainTrigger(TagCommand.class) + "\n"
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

        eb.addField("", "**Head over to** " + link + " **for the full commands reference" +
                " or run **`w.help [command]`** for detailed information on a command.**", false);

        commandInfo.reply(eb.build());
        return true;
    }
}
