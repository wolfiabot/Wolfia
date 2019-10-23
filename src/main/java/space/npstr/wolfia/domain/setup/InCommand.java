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

package space.npstr.wolfia.domain.setup;

import net.dv8tion.jda.api.entities.User;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.ban.BanService;
import space.npstr.wolfia.domain.room.PrivateRoomService;
import space.npstr.wolfia.game.definitions.Games;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by npstr on 23.08.2016
 */
@Command
public class InCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "in";

    private final BanService banService;
    private final GameSetupService gameSetupService;
    private final PrivateRoomService privateRoomService;
    private final GameSetupRender render;

    public InCommand(BanService banService, GameSetupService gameSetupService, PrivateRoomService privateRoomService,
                     GameSetupRender render) {
        this.banService = banService;
        this.gameSetupService = gameSetupService;
        this.privateRoomService = privateRoomService;
        this.render = render;
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
        if (this.privateRoomService.guild(context.guild.getIdLong()).isPrivateRoom()) {
            context.replyWithMention("you can't play games in a private guild.");
            return false;
        }

        GameSetupService.Action setupAction = this.gameSetupService.channel(context.textChannel.getIdLong());
        //force in by bot owner ( ͡° ͜ʖ ͡°)
        List<User> mentionedUsers = context.getMessage().getMentionedUsers();
        if (!mentionedUsers.isEmpty() && context.isOwner()) {
            Set<Long> userIds = mentionedUsers.stream()
                    .map(User::getIdLong)
                    .collect(Collectors.toSet());
            setupAction.inUsers(userIds);

            GameSetup setup = setupAction.cleanUpInnedPlayers(context.getJda().getShardManager());
            context.reply(this.render.render(setup, context));
            return true;
        }

        if (this.banService.isBanned(context.invoker.getIdLong())) {
            context.replyWithMention("lol ur banned.");
            return false;
        }

        if (setupAction.getOrDefault().isIn(context.invoker.getIdLong())) {
            context.replyWithMention("you have inned already.");
            return false;
        }
        setupAction.inUser(context.invoker.getIdLong());
        GameSetup setup = setupAction.cleanUpInnedPlayers(context.getJda().getShardManager());
        context.reply(this.render.render(setup, context));
        return true;
    }
}
