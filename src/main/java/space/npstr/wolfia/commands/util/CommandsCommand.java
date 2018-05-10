package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.EmbedBuilder;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.Context;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import javax.annotation.Nonnull;

public class CommandsCommand extends BaseCommand {

    public CommandsCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
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
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_IN + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_OUT + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_SETUP + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_START + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_ROLEPM + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_STATUS + "\n"
                ;

        final String ingameCommands = ""
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_SHOOT + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_VOTE + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_UNVOTE + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_VOTECOUNT + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_NIGHTKILL + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_CHECK + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_HOHOHO + " _(xmas mode only)_\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_ITEMS + " _(xmas mode only)_\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_OPENPRESENT + " _(xmas mode only)_\n"
                ;

        final String settingsCommands =
                  WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_CHANNELSETTINGS
                ;

        final String statsCommands = ""
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_USERSTATS + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_GUILDSTATS + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_BOTSTATS + "\n"
                ;

        final String otherCommands = ""
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_COMMANDS + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_HELP + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_INFO + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_INVITE + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_RANK + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_REPLAY + "\n"
                + WolfiaConfig.DEFAULT_PREFIX + CommRegistry.COMM_TRIGGER_TAG + "\n"
                ;
        //@formatter:on


        final String link = App.DOCS_LINK + "#commands";
        final EmbedBuilder eb = Context.getDefaultEmbedBuilder()
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
