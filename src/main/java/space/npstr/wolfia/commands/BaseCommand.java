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


import net.dv8tion.jda.core.entities.User;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by npstr on 23.08.2016
 */
public interface BaseCommand {

    /**
     * @return the commands main trigger
     */
    String getTrigger();

    /**
     * @return the commands aliases
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * Execute the command
     */
    boolean execute(@Nonnull CommandContext context) throws IllegalGameStateException;

    /**
     * @return a help string that should explain the usage of this command
     */
    @Nonnull
    String help();

    /**
     * @return a help string with aliases, if there are any
     */
    default String getHelp() {
        if (!getAliases().isEmpty()) {
            final List<String> prefixedAliases = getAliases().stream()
                    .map(alias -> WolfiaConfig.DEFAULT_PREFIX + alias)
                    .collect(Collectors.toList());
            return help() + "\nAlias: " + String.join(", ", prefixedAliases);
        } else {
            return help();
        }
    }

    /**
     * @return a better formatted representation of a commands help
     */
    default String formatHelp(final User invoker) {
        return String.format("%s, I did not understand that input. Here's some help:%n%s",
                invoker.getAsMention(), TextchatUtils.asMarkdown(getHelp()));
    }

    /**
     * @return how to invoke this command with its main trigger
     */
    @Nonnull
    default String invocation() {
        return WolfiaConfig.DEFAULT_PREFIX + getTrigger();
    }
}
