/*
 * Copyright (C) 2016-2025 the original author or authors
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.domain.room.PrivateRoomService;

import static java.util.Objects.requireNonNull;

@Command
public class OutCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "out";

    private final GameSetupService gameSetupService;
    private final PrivateRoomService privateRoomService;
    private final GameSetupRender render;
    private final GameRegistry gameRegistry;

    public OutCommand(GameSetupService gameSetupService, PrivateRoomService privateRoomService, GameSetupRender render,
                      GameRegistry gameRegistry) {

        this.gameSetupService = gameSetupService;
        this.privateRoomService = privateRoomService;
        this.render = render;
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("leave");
    }

    @Override
    public String help() {
        return invocation() + " [@user]"
                + "\n#Remove you from the current signup list. Moderators can out other players by mentioning them.";
    }

    @Override
    public boolean execute(CommandContext commandContext) {
        GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        if (this.gameRegistry.get(context.textChannel) != null) {
            context.replyWithMention("please sign up/sign out for the next game after the current one is over.");
            return false;
        }

        //check for private guilds where we dont want games to be started
        if (this.privateRoomService.guild(context.guild.getIdLong()).isPrivateRoom()) {
            context.replyWithMention("you can't play games in a private guild.");
            return false;
        }

        long channelId = context.textChannel.getIdLong();
        GameSetupService.Action setupAction = this.gameSetupService.channel(channelId);
        //is this a forced out of a player by an moderator or the bot owner?
        List<User> mentionedUsers = context.getMessage().getMentions().getUsers();
        if (!mentionedUsers.isEmpty()) {
            if (!context.member.hasPermission(context.textChannel, Permission.MESSAGE_MANAGE) && !context.isOwner()) {
                context.replyWithMention("you need to have the following permission in this channel to be able to out players: "
                        + "**" + Permission.MESSAGE_MANAGE.name() + "**");
                return false;
            } else {
                Set<Long> userIds = mentionedUsers.stream()
                        .map(User::getIdLong)
                        .collect(Collectors.toSet());
                setupAction.outUsers(userIds);
                GameSetup setup = setupAction.cleanUpInnedPlayers(requireNonNull(context.getJda().getShardManager()));
                context.reply(this.render.render(setup, context));
                return true;
            }
        } else {
            if (setupAction.getOrDefault().isIn(context.invoker.getIdLong())) {
                //handling a regular out
                setupAction.outUser(context.invoker.getIdLong());
                GameSetup setup = setupAction.cleanUpInnedPlayers(requireNonNull(context.getJda().getShardManager()));
                context.reply(this.render.render(setup, context));
                return true;
            }
        }
        return false;
    }

}
