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
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.PrivateGuild;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

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

    @Override
    public String help() {
        return "Register a private guild.";
    }

    @Override
    public synchronized boolean execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {

        final MessageReceivedEvent e = commandInfo.event;
        final Guild g = e.getGuild();

        //make sure we have admin rights
        if (!g.getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
            Wolfia.handleOutputMessage(e.getTextChannel(), "%s, gimme admin perms first.", e.getAuthor().getAsMention());
            return false;
        }

        //set up the looks
        //- give the server a name
        g.getManager().setName("Wolfia Private Server").queue(null, Wolfia.defaultOnFail());
        //- give the server a logo
        try {
            g.getManager().setIcon(Icon.from(getClass().getResourceAsStream("/img/popcorn_mafia_guy.png"))).queue(null, Wolfia.defaultOnFail());
        } catch (final IOException ex) {
            log.error("Could not set icon for guild {}", g.getIdLong(), e);
            return false;
        }

        //set up rights:
        //- deny creating invites
        //- deny reading messages
        g.getPublicRole().getManager().revokePermissions(Permission.CREATE_INSTANT_INVITE, Permission.MESSAGE_READ).queue(null, Wolfia.defaultOnFail());
        //- delete #general
        for (final TextChannel tc : g.getTextChannels()) {
            tc.delete().reason("Preparing private guild for usage").complete();
        }


        //register it
        //setting the private guild number in this manual way is ugly, but Hibernate/JPA are being super retarded about autogenerating non-ids
        //since this command should only run occasionally, and never in some kind of race condition (fingers crossed), I will allow this
        final PrivateGuild pg = new PrivateGuild(DbWrapper.loadPrivateGuilds().size(), g.getIdLong());
        DbWrapper.persist(pg);
        Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.add(pg);
        Wolfia.addEventListener(pg);
        Wolfia.getInstance().commandListener.addIgnoredGuild(pg.getId());
        g.getManager().setName("Wolfia Private Server #" + pg.getPrivateGuildNumber()).queue(null, Wolfia.defaultOnFail());
        return true;
    }
}
