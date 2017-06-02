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

package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.User;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.utils.App;

import static space.npstr.wolfia.Wolfia.jda;

/**
 * Created by napster on 28.05.17.
 * <p>
 * Thanks Fred
 */
public class InfoCommand implements ICommand {

    public static final String COMMAND = "info";

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {
        String maStats = "```\n";
        maStats += "Reserved memory:        " + Runtime.getRuntime().totalMemory() / 1000000 + "MB\n";
        maStats += "-> Of which is used:    " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + "MB\n";
        maStats += "-> Of which is free:    " + Runtime.getRuntime().freeMemory() / 1000000 + "MB\n";
        maStats += "Max reservable:         " + Runtime.getRuntime().maxMemory() / 1000000 + "MB\n";
        maStats += "```";


        String botInfo = "```\n";
        botInfo += "Games being played:     " + Games.getAll().size() + "\n";
        botInfo += "Known servers:          " + jda.getGuilds().size() + "\n";
        botInfo += "Known users in servers: " + jda.getUsers().size() + "\n";
        botInfo += "Version:                " + App.VERSION + "\n";
        botInfo += "JDA responses total:    " + jda.getResponseTotal() + "\n";
        botInfo += "JDA version:            " + JDAInfo.VERSION + "\n";
        botInfo += "Bot owner:              " + jda.getUserById(App.OWNER_ID).getName() + "\n";
        botInfo += "```";

        final EmbedBuilder eb = new EmbedBuilder();
        final User self = Wolfia.jda.getSelfUser();
        eb.setThumbnail(self.getEffectiveAvatarUrl());
        eb.setAuthor(self.getName(), App.WEBSITE, self.getEffectiveAvatarUrl());
        eb.setTitle(self.getName() + " General Stats", App.WEBSITE);
        eb.setDescription(App.DESCRIPTION);
        eb.addField("Bot info", botInfo, false);
        eb.addField("Machine stats", maStats, false);


        commandInfo.event.getChannel().sendMessage(eb.build()).queue();
    }

    @Override
    public String help() {
        return "Show some statistics about this bot.";
    }
}
