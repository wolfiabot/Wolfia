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

import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.User;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.game.definitions.Games;

import javax.annotation.Nonnull;


/**
 * Created by napster on 28.05.17.
 * <p>
 * Thanks Fred
 */
@Component
public class InfoCommand implements BaseCommand {

    public static final String TRIGGER = "info";

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Nonnull
    @Override
    public String help() {
        return invocation()
                + "\n#Show some statistics about this bot.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) {
        context.getEvent().getJDA().asBot().getShardManager().getApplicationInfo().submit()
                .thenApply(ApplicationInfo::getDescription)
                .thenAccept(description -> execute(context, description));
        return true;
    }

    private void execute(@Nonnull final CommandContext context, String description) {
        final User owner = Wolfia.getUserById(App.OWNER_ID);
        String maStats = "```\n";
        maStats += "Reserved memory:        " + Runtime.getRuntime().totalMemory() / 1000000 + "MB\n";
        maStats += "-> Of which is used:    " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + "MB\n";
        maStats += "-> Of which is free:    " + Runtime.getRuntime().freeMemory() / 1000000 + "MB\n";
        maStats += "Max reservable:         " + Runtime.getRuntime().maxMemory() / 1000000 + "MB\n";
        maStats += "```";


        String botInfo = "```\n";
        botInfo += "Games being played:     " + Games.getRunningGamesCount() + "\n";
        botInfo += "Known servers:          " + Wolfia.getGuildsAmount() + "\n";
        botInfo += "Known users in servers: " + Wolfia.getUsersAmount() + "\n";
        botInfo += "Version:                " + App.VERSION + "\n";
        botInfo += "JDA responses total:    " + Wolfia.getResponseTotal() + "\n";
        botInfo += "JDA version:            " + JDAInfo.VERSION + "\n";
        if (owner != null) {
            botInfo += "Bot owner:              " + owner.getName() + "#" + owner.getDiscriminator() + "\n";
        }
        botInfo += "```";

        final EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        final User self = Wolfia.getSelfUser();
        eb.setThumbnail(self.getEffectiveAvatarUrl());
        eb.setAuthor(self.getName(), App.SITE_LINK, self.getEffectiveAvatarUrl());
        eb.setTitle(self.getName() + " General Stats", App.SITE_LINK);
        eb.setDescription(description);
        eb.addField("Bot info", botInfo, false);
        eb.addField("Machine stats", maStats, false);


        context.reply(eb.build());
    }
}
