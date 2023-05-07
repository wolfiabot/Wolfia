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

import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.commands.util.HelpCommand;

@Component
public class CommRegistry {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommRegistry.class);

    private final List<BaseCommand> commands = new ArrayList<>();

    public CommRegistry(List<BaseCommand> comms) {
        comms.forEach(this::registerCommand);
        registerCommand(new HelpCommand(this));
    }

    @Nullable
    public BaseCommand getCommand(String input) {
        return this.commands.stream()
                .filter(command -> input.equalsIgnoreCase(command.getTrigger()) || command.getAliases().contains(input.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    private void registerCommand(BaseCommand command) {
        List<String> allTriggers = new ArrayList<>();
        allTriggers.add(command.getTrigger());
        allTriggers.addAll(command.getAliases());
        for (String trigger : allTriggers) {
            if (getCommand(trigger) != null) {
                log.error("Duplicate command trigger: {}", trigger);
            }
        }
        this.commands.add(command);
    }
}
