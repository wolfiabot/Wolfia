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

package space.npstr.wolfia.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import java.util.Arrays;
import java.util.regex.Pattern;

@Component
public class CommandContextParser {

    /**
     * @param event the event to be parsed
     * @return The full context for the triggered command, or null if it's not a command that we know.
     */
    public CommandContext parse(CommRegistry commRegistry, MessageReceivedEvent event) {

        String raw = event.getMessage().getContentRaw();
        String input;

        String prefix = WolfiaConfig.DEFAULT_PREFIX;
        if (raw.toLowerCase().startsWith(prefix.toLowerCase())) {
            input = raw.substring(prefix.length());
        } else {
            return null;
        }

        input = input.trim();// eliminate possible whitespace between the prefix and the rest of the input
        if (input.isEmpty()) {
            return null;
        }

        //split by any length of white space characters
        // the \p{javaSpaceChar} instead of the better known \s is used because it actually includes unicode whitespaces
        String[] args = input.split("\\p{javaSpaceChar}+");
        if (args.length < 1) {
            return null; //while this shouldn't technically be possible due to the preprocessing of the input, better be safe than throw exceptions
        }

        String commandTrigger = args[0];
        BaseCommand command = commRegistry.getCommand(commandTrigger.toLowerCase());

        if (command == null) {
            return null;
        } else {
            return new CommandContext(event, commandTrigger,
                    Arrays.copyOfRange(args, 1, args.length),//exclude args[0] that contains the command trigger
                    input.replaceFirst(Pattern.quote(commandTrigger), "").trim(),
                    command
            );
        }
    }
}
