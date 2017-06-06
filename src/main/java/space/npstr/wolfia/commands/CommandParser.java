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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.Config;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by npstr on 23.08.2016
 */
public class CommandParser {

    public static CommandContainer parse(final String rw, final MessageReceivedEvent e) {
        final ArrayList<String> split = new ArrayList<>();
        final String beheaded = rw.substring(Config.PREFIX.length()).trim();
        final String[] splitBeheaded = beheaded.split(" ");
        Collections.addAll(split, splitBeheaded);
        final String command = split.get(0).toLowerCase();
        final String[] args = new String[split.size() - 1];
        split.subList(1, split.size()).toArray(args);

        return new CommandContainer(rw, beheaded, splitBeheaded, command, args, e);
    }

    public static class CommandContainer {
        public final String raw; //the full thing
        public final String beheaded; //without the prefix
        public final String[] splitBeheaded; //without the prefix, split by " "
        public final String command; // the actual command, in lower case
        public final String[] args; // the actual arguments
        public final MessageReceivedEvent event; //underlying event

        public CommandContainer(final String rw, final String beheaded, final String[] splitBeheaded, final String command, final String[] args, final MessageReceivedEvent e) {
            this.raw = rw;
            this.beheaded = beheaded;
            this.splitBeheaded = splitBeheaded;
            this.command = command;
            this.args = args;
            this.event = e;
        }
    }
}
