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

package space.npstr.wolfia.commands.util;

import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandCategory;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;

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

    @Override
    public String help() {
        return invocation()
                + "\n#Show all available commands.";
    }

    @Override
    public boolean execute(CommandContext context) {
        String link = App.DOCS_LINK + "/commands";
        EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder()
                .setTitle("Wolfia commands", link);

        for (CommandCategory category : CommandCategory.values()) {
            String commands = category.triggers().stream()
                    .map(trigger -> WolfiaConfig.DEFAULT_PREFIX + trigger
                            + (CommandCategory.isXmasOnly(trigger) ? XMAS_MODE_ONLY : ""))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            eb.addField(category.displayName(), commands, true);
        }

        eb.addBlankField(true)
                .addField("", "**Head over to** " + link + " **for the full commands reference" +
                        " or run **`w.help [command]`** for detailed information on a command.**", false);

        context.reply(eb.build());
        return true;
    }
}
