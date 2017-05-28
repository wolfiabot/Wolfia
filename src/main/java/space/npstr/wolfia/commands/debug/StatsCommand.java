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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.MessageBuilder;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.ICommand;
import space.npstr.wolfia.commands.meta.IOwnerRestricted;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.utils.App;

/**
 * Created by napster on 28.05.17.
 * <p>
 * Thanks Fred
 */
public class StatsCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "stats";

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {
        final long totalSecs = (System.currentTimeMillis() - Wolfia.START_TIME) / 1000;
        final int days = (int) (totalSecs / (60 * 60 * 24));
        final int hours = (int) ((totalSecs / (60 * 60)) % 24);
        final int mins = (int) ((totalSecs / 60) % 60);
        final int secs = (int) (totalSecs % 60);

        String str = new MessageBuilder().appendFormat("Bot has been running for %s days and %02dh %02dm %02ds.\n", days, hours, mins, secs).build().getRawContent();

        str = str + "\n```";

        str = str + "Reserved memory:                " + Runtime.getRuntime().totalMemory() / 1000000 + "MB\n";
        str = str + "-> Of which is used:            " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + "MB\n";
        str = str + "-> Of which is free:            " + Runtime.getRuntime().freeMemory() / 1000000 + "MB\n";
        str = str + "Max reservable:                 " + Runtime.getRuntime().maxMemory() / 1000000 + "MB\n";

        str = str + "\n----------\n\n";

        str = str + "Games being  played:            " + Games.getAll().size() + "\n";
        str = str + "Known servers:                  " + Wolfia.jda.getGuilds().size() + "\n";
        str = str + "Known users in servers:         " + Wolfia.jda.getUsers().size() + "\n";
        str = str + "Version:                        " + App.VERSION + "\n";
        str = str + "JDA responses total:            " + Wolfia.jda.getResponseTotal() + "\n";
        str = str + "JDA version:                    " + JDAInfo.VERSION;

        str = str + "```";

        commandInfo.event.getChannel().sendMessage(commandInfo.event.getMember().getAsMention() + ": " + str).queue();
    }

    @Override
    public String help() {
        return "Show some statistics about this bot.";
    }
}
