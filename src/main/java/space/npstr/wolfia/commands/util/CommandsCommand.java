package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.EmbedBuilder;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.MessageContext;
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
import space.npstr.wolfia.config.properties.WolfiaConfig;

import javax.annotation.Nonnull;
import java.util.List;

@Component
public class CommandsCommand implements BaseCommand {

    public static final String TRIGGER = "commands";

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
                + WolfiaConfig.DEFAULT_PREFIX + HohohoCommand.TRIGGER + " _(xmas mode only)_\n"
                + WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER + " _(xmas mode only)_\n"
                + WolfiaConfig.DEFAULT_PREFIX + OpenPresentCommand.TRIGGER + " _(xmas mode only)_\n"
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
