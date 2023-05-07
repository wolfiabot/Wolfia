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

package space.npstr.wolfia.commands.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.system.ApplicationInfoProvider;

import static java.util.Objects.requireNonNull;

@Command
public class InfoCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "info";

    private final GameRegistry gameRegistry;

    public InfoCommand(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public String help() {
        return invocation()
                + "\n#Show some statistics about this bot.";
    }

    @Override
    public boolean execute(CommandContext context) {
        ShardManager shardManager = context.getJda().getShardManager();
        requireNonNull(shardManager).retrieveApplicationInfo().submit()
                .thenApply(ApplicationInfo::getDescription)
                .thenAccept(description -> execute(context, description));
        return true;
    }

    private void execute(CommandContext context, String description) {
        ShardManager shardManager = requireNonNull(context.getJda().getShardManager());
        var appInfoProvider = new ApplicationInfoProvider(context.getJda().getShardManager());
        User owner = appInfoProvider.getOwner();
        String maStats = "```\n";
        maStats += "Reserved memory:        " + Runtime.getRuntime().totalMemory() / 1000000 + "MB\n";
        maStats += "-> Of which is used:    " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + "MB\n";
        maStats += "-> Of which is free:    " + Runtime.getRuntime().freeMemory() / 1000000 + "MB\n";
        maStats += "Max reservable:         " + Runtime.getRuntime().maxMemory() / 1000000 + "MB\n";
        maStats += "```";


        String botInfo = "```\n";
        botInfo += "Games being played:     " + this.gameRegistry.getRunningGamesCount() + "\n";
        botInfo += "Known servers:          " + shardManager.getGuildCache().size() + "\n";
        //UnifiedShardCacheViewImpl#stream calls distinct for us
        botInfo += "Known users in servers: " + shardManager.getUserCache().stream().count() + "\n";
        botInfo += "Version:                " + App.VERSION + "\n";
        botInfo += "JDA responses total:    " + shardManager.getShards().stream().mapToLong(JDA::getResponseTotal).sum() + "\n";
        botInfo += "JDA version:            " + JDAInfo.VERSION + "\n";
        botInfo += "Bot owner:              " + owner.getName() + "#" + owner.getDiscriminator() + "\n";
        botInfo += "```";

        EmbedBuilder eb = MessageContext.getDefaultEmbedBuilder();
        User self = context.event.getJDA().getSelfUser();
        eb.setThumbnail(self.getEffectiveAvatarUrl());
        eb.setAuthor(self.getName(), App.SITE_LINK, self.getEffectiveAvatarUrl());
        eb.setTitle(self.getName() + " General Stats", App.SITE_LINK);
        eb.setDescription(description);
        eb.addField("Bot info", botInfo, false);
        eb.addField("Machine stats", maStats, false);


        context.reply(eb.build());
    }
}
