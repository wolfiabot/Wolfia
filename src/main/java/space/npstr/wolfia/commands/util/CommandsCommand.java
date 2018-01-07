package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.EmbedBuilder;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.Context;

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
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_IN + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_OUT + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_SETUP + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_START + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_ROLEPM + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_STATUS + "\n"
                ;

        final String ingameCommands = ""
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_SHOOT + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_VOTE + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_UNVOTE + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_VOTECOUNT + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_NIGHTKILL + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_CHECK + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_HOHOHO + " _(xmas mode only)_\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_ITEMS + " _(xmas mode only)_\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_OPENPRESENT + " _(xmas mode only)_\n"
                ;

        final String settingsCommands =
                  Config.PREFIX + CommRegistry.COMM_TRIGGER_CHANNELSETTINGS
                ;

        final String statsCommands = ""
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_USERSTATS + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_GUILDSTATS + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_BOTSTATS + "\n"
                ;

        final String otherCommands = ""
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_COMMANDS + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_HELP + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_INFO + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_INVITE + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_RANK + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_REPLAY + "\n"
                + Config.PREFIX + CommRegistry.COMM_TRIGGER_TAG + "\n"
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
