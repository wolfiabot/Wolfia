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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.room.PrivateRoom;
import space.npstr.wolfia.domain.room.PrivateRoomService;
import space.npstr.wolfia.utils.discord.RestActions;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Created by napster on 11.06.17.
 * <p>
 * //this command will register a guild for use as a place to provide private communications, like wolfchat
 */
@Command
public class RegisterPrivateRoomCommand implements BaseCommand {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegisterPrivateRoomCommand.class);

    private final PrivateRoomService privateRoomService;

    public RegisterPrivateRoomCommand(PrivateRoomService privateRoomService) {
        this.privateRoomService = privateRoomService;
    }

    @Override
    public String getTrigger() {
        return "register";
    }

    @Nonnull
    @Override
    public String help() {
        return "Register a guild as a private room.";
    }

    @Override
    public synchronized boolean execute(@Nonnull final CommandContext commandContext) {

        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        //make sure we have admin rights
        if (!context.guild.getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
            context.replyWithMention("gimme admin perms first.");
            return false;
        }

        //set up the looks
        //- give the server a name
        context.guild.getManager().setName("Wolfia Private Server").queue(null, RestActions.defaultOnFail());
        //- give the server a logo
        try {
            context.guild.getManager().setIcon(Icon.from(getClass().getResourceAsStream("/img/popcorn_mafia_guy.png"))).queue(null, RestActions.defaultOnFail());
        } catch (final IOException e) {
            log.error("Could not set icon for guild {}", context.guild.getIdLong(), e);
            return false;
        }

        //set up rights:
        //- deny creating invites
        //- deny reading messages
        context.guild.getPublicRole().getManager().revokePermissions(Permission.CREATE_INSTANT_INVITE, Permission.MESSAGE_READ).queue(null, RestActions.defaultOnFail());
        //- delete #general
        for (final TextChannel tc : context.guild.getTextChannels()) {
            tc.delete().reason("Preparing private guild for usage").complete();
        }


        //register it
        Optional<PrivateRoom> registered = this.privateRoomService.guild(context.guild.getIdLong()).register();
        if (registered.isEmpty()) {
            context.replyWithMention("Looks like this guild is already registered as a private room.");
            return false;
        }

        PrivateRoom privateRoom = registered.get();
        ShardManager shardManager = context.getJda().getShardManager();
        requireNonNull(shardManager).addEventListener(privateRoom);
        String name = "Wolfia Private Server #" + privateRoom.getNumber();
        context.guild.getManager().setName(name)
                .queue(null, RestActions.defaultOnFail());
        return true;
    }
}
