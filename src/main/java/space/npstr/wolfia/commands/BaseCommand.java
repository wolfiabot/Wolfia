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
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by npstr on 23.08.2016
 */
public abstract class BaseCommand {

    private final List<String> commandTriggers;

    public BaseCommand(final String trigger, final String... aliases) {
        this.commandTriggers = new ArrayList<>();
        this.commandTriggers.add(trigger);
        this.commandTriggers.addAll(Arrays.asList(aliases));
    }

    //executes the command
    protected abstract boolean execute(CommandParser.CommandContainer commandInfo)
            throws IllegalGameStateException, DatabaseException;

    //return a help string that should explain the usage of this command
    protected abstract String help();

    //returns a help string with aliases, if there are any
    public String getHelp() {
        final List<String> aliases = new ArrayList<>();

        for (final String trigger : this.commandTriggers) {
            if (trigger.equals(getMainTrigger())) {
                continue;
            }
            aliases.add(Config.PREFIX + trigger);
        }

        if (!aliases.isEmpty()) {
            return help() + "\nAlias: " + String.join(", ", aliases);
        } else {
            return help();
        }
    }

    //will return a better formatted representation of a commands help
    public String formatHelp(final User invoker) {
        return String.format("%s, I did not understand that input. Here's some help:%n%s",
                invoker.getAsMention(), TextchatUtils.asMarkdown(getHelp()));
    }

    //returns the main string representing the command which was the former COMMAND fields
    public String getMainTrigger() {
        return this.commandTriggers.get(0);
    }

    public List<String> commandTriggers() {
        return this.commandTriggers;
    }
}
