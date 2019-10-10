/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

package space.npstr.wolfia.commands.game;

import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.db.entities.Setup;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.ban.BanService;
import space.npstr.wolfia.game.definitions.Games;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by npstr on 23.08.2016
 */
@Command
public class InCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "in";

    private final BanService banService;

    public InCommand(BanService banService) {
        this.banService = banService;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("join");
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + "\n#Add you to the signup list for this channel. You will play in the next starting game.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) {

        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        //is there a game going on?
        if (Games.get(context.textChannel) != null) {
            context.replyWithMention("the game has already started! Please wait until it is over to join.");
            return false;
        }

        //check for private guilds where we dont want games to be started
        if (PrivateGuild.isPrivateGuild(context.guild)) {
            context.replyWithMention("you can't play games in a private guild.");
            return false;
        }

        final EntityKey<Long, Setup> setupKey = Setup.key(context.textChannel.getIdLong());
        //force in by bot owner ( ͡° ͜ʖ ͡°)
        if (!context.msg.getMentionedUsers().isEmpty() && context.isOwner()) {
            final Setup s = Launcher.getBotContext().getDatabase().getWrapper().findApplyAndMerge(setupKey,
                    setup -> {
                        for (final User u : context.msg.getMentionedUsers()) {
                            setup.inUser(u.getIdLong());
                        }
                        return setup;
                    });
            context.reply(s.getStatus(context));
            return true;
        }

        if (this.banService.isBanned(context.invoker.getIdLong())) {
            context.replyWithMention("lol ur banned.");
            return false;
        }

        if (Launcher.getBotContext().getDatabase().getWrapper().getOrCreate(setupKey).isInned(context.invoker.getIdLong())) {
            context.replyWithMention("you have inned already.");
            return false;
        }
        final Setup s = Launcher.getBotContext().getDatabase().getWrapper().findApplyAndMerge(setupKey, setup -> setup.inUser(context.invoker.getIdLong()));
        context.reply(s.getStatus(context));
        return true;
    }
}
