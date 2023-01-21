/*
 * Copyright (C) 2016-2020 the original author or authors
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

package space.npstr.wolfia.commands.ingame;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import net.dv8tion.jda.api.entities.ChannelType;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

@Command
public class ItemsCommand extends GameCommand {

    public static final String TRIGGER = "items";

    public ItemsCommand(GameRegistry gameRegistry) {
        super(gameRegistry);
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @NonNull
    @Override
    public String help() {
        return invocation() + "\n#Show your items.";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean execute(@NonNull final CommandContext context) throws IllegalGameStateException {
        //this command is expected to be called by a player in a private channel

        if (context.channel.getType() != ChannelType.PRIVATE) {
            context.replyWithMention("items can only be checked in private messages.");
            return false;
        }

        //todo handle a player being part of multiple games properly
        boolean issued = false;
        for (final Game g : this.gameRegistry.getAll().values()) {
            if (g.isUserPlaying(context.invoker) && g.isLiving(context.invoker)) {
                final Player p = g.getPlayer(context.invoker);
                if (p.items.isEmpty()) {
                    context.reply("You don't possess any items.");
                } else {
                    final List<String> itemsList = p.items.stream().map(i -> i.itemType.emoji + ": " + i.itemType.explanation).collect(Collectors.toList());
                    context.reply("You have the following items:\n" + String.join("\n", itemsList));
                }
                issued = true;
            }
        }
        if (!issued) {
            context.replyWithMention(String.format("you aren't alive and in any ongoing game currently. Say `%s` to get started!",
                    WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER));
            return false;
        }
        return true;
    }
}
