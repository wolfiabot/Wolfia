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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.PrivateGuild;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.RoleUtils;

import java.io.IOException;

/**
 * Created by napster on 11.06.17.
 * <p>
 * //this command will register a guild for use as a place to provide private communications, like wolfchat
 */
public class RegisterPrivateServerCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "register";

    private static final Logger log = LoggerFactory.getLogger(RegisterPrivateServerCommand.class);

    @Override
    public String help() {
        return "todo"; //todo
    }

    @Override
    public synchronized void execute(final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {

        final MessageReceivedEvent e = commandInfo.event;
        final Guild g = e.getGuild();

        //make sure we have admin rights
        if (!g.getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
            Wolfia.handleOutputMessage(e.getTextChannel(), "%s, gimme admin perms first.", e.getAuthor().getAsMention());
            return;
        }

        //set up the looks
        //- give the server a name
        g.getManager().setName("Wolfia Private Server").queue();
        //- give the server a logo
        try {
            g.getManager().setIcon(Icon.from(getClass().getResourceAsStream("/img/popcorn_mafia_guy.png"))).queue();
        } catch (final IOException ex) {
            log.error("Could not set icon for guild {}", g.getIdLong(), e);
            return;
        }

        //set up rights:
        //- deny creating invites
        //- deny reading messages
        g.getPublicRole().getManager().revokePermissions(Permission.CREATE_INSTANT_INVITE, Permission.MESSAGE_READ).queue();
        //- deny writing messages in #general
        RoleUtils.deny(g.getPublicChannel(), g.getPublicRole(), Permission.MESSAGE_WRITE).queue();

        //set up #general
        //- post a message about welcoming the scum team, and their channel being set up (just click it on the left side etc.)


        //register it
        //setting the private guild number in this manual way is ugly, but Hibernate/JPA are being super retarded about autogenerating non-ids
        //since this command should only run occasionally, and never in some kind of race condition (fingers crossed), I will allow this
        final PrivateGuild pg = new PrivateGuild(DbWrapper.loadPrivateGuilds().size(), g.getIdLong());
        DbWrapper.persist(pg);
        Wolfia.FREE_PRIVATE_GUILD_QUEUE.add(pg);
        Wolfia.jda.addEventListener(pg);
        Wolfia.wolfia.commandListener.addIgnoredGuild(pg.getId());
        g.getManager().setName("Wolfia Private Server #" + pg.getPrivateGuildNumber()).queue();
    }
}