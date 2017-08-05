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


import net.dv8tion.jda.core.entities.Member;
import space.npstr.wolfia.game.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by npstr on 23.08.2016
 */
public abstract class BaseCommand {

    //executes the command
    protected abstract boolean execute(CommandParser.CommandContainer commandInfo) throws IllegalGameStateException;

    //return a help string that should explain the usage of this command
    public abstract String help();

    //will return a better formatted representation of a commands help
    public String formatHelp(final Member invoker) {
        return String.format("%s, I did not understand that input. Here's some help:\n%s",
                invoker.getAsMention(), TextchatUtils.asMarkdown(help()));
    }
}
