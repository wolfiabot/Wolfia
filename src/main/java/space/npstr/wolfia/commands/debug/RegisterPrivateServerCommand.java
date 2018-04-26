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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.utils.discord.RestActions;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Created by napster on 11.06.17.
 * <p>
 * //this command will register a guild for use as a place to provide private communications, like wolfchat
 */
public class RegisterPrivateServerCommand extends BaseCommand implements IOwnerRestricted {

    public RegisterPrivateServerCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    private static final Logger log = LoggerFactory.getLogger(RegisterPrivateServerCommand.class);

    @Nonnull
    @Override
    public String help() {
        return "Register a private guild.";
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
        //setting the private guild number in this manual way is ugly, but Hibernate/JPA are being super retarded about autogenerating non-ids
        //since this command should only run occasionally, and never in some kind of race condition (fingers crossed), I will allow this
        final DatabaseWrapper dbWrapper = Wolfia.getDatabase().getWrapper();
        try {
            final int number = Math.toIntExact(dbWrapper.selectJpqlQuery("SELECT COUNT (pg) FROM PrivateGuild pg", Long.class).get(0));
            PrivateGuild pg = new PrivateGuild(number, context.guild.getIdLong());
            pg = dbWrapper.persist(pg);
            Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.add(pg);
            Wolfia.addEventListener(pg);
            context.guild.getManager().setName("Wolfia Private Server #" + pg.getPrivateGuildNumber()).queue(null, RestActions.defaultOnFail());
        } catch (final DatabaseException e) {
            log.error("Db blew up saving private guild", e);
            return false;
        }
        return true;
    }
}
