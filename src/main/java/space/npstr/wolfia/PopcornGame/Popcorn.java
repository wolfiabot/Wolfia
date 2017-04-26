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

package space.npstr.wolfia.PopcornGame;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.*;
import space.npstr.wolfia.utils.CommandParser;
import space.npstr.wolfia.utils.Roles;

import java.util.*;

/**
 * Created by npstr on 22.10.2016
 */
public class Popcorn implements Game, CommandHandler {

    private final TextChannel channel;

    private final static Logger log = LoggerFactory.getLogger(Popcorn.class);

    private final String PREFIX = "!";

    public String getPREFIX() {
        return PREFIX;
    }

    public Popcorn(TextChannel channel) {
        this.channel = channel;
    }

    private void prepareChannel(Set<String> players) throws PermissionException {

        // - ensure write access for the bot in the game channel
        Role botRole = Roles.getOrCreateRole(channel.getGuild(), Config.BOT_ROLE_NAME);
        channel.getGuild().getController().addRolesToMember(channel.getGuild().getMemberById(Main.jda.getSelfUser().getId()), botRole).complete();

        Roles.grant(channel, botRole, Permission.MESSAGE_WRITE, true);


        // - read only access for @everyone in the game channel
        Roles.grant(channel, channel.getGuild().getPublicRole(), Permission.MESSAGE_WRITE, false);


        // - write permission for the players
        Roles.deleteRole(channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
        Role playerRole = Roles.getOrCreateRole(channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
        Roles.grant(channel, playerRole, Permission.MESSAGE_WRITE, true);

        for (String userId : players) {
            channel.getGuild().getController().addRolesToMember(channel.getGuild().getMemberById(userId), playerRole).complete();
        }


        // - revoke writing rights on the discord server for players during the game?
        playerRole.getManager().revokePermissions(Permission.MESSAGE_WRITE).complete();

        //TODO
        // - add player list command
        // - add shoot command
    }


    /**
     * @return amount of players this game needs to start
     */
    @Override
    public int getAmountOfPlayers() {
        if (Config.C.isDebug) return 1;
        else return 11;
    }

    @Override
    public void start(Set<String> players) {
        try {
            prepareChannel(players);
        } catch (PermissionException e) {
            log.warn("Could not prepare channel " + channel.getName() + ", id: " + channel.getId() + ", due to missing" +
                    " permission: " + e.getPermission().name());
            e.printStackTrace();

            String out = "The bot is missing the permission " + e.getPermission().name() + " to run the game in here." +
                    "\nStart aborted.";
            Main.handleOutputMessage(channel, out);
            return;
        }


        // - rand the roles
        List<String> rand = new ArrayList<>(players);
        Collections.shuffle(rand);
        Set<String> woofs;
        Set<String> villagers;
        if (Config.C.isDebug) {
            woofs = new HashSet<>(rand.subList(0, 1));
            villagers = new HashSet<>();
        } else {
            woofs = new HashSet<>(rand.subList(0, 4));//first 4 players on the shuffled list are the woofs
            villagers = new HashSet<>(rand.subList(4, rand.size() - 1));
        }
        //TODO
        //TODO bots aren't allowed to create group DMs which sucks; applying for white listing possible, but currently
        //TODO (2.2.1) no JDA support
        //source: https://discordapp.com/developers/docs/resources/channel#group-dm-add-recipient
        //workaround: echo the messages of the players into their PMs
        // - rand the gun/let mafia vote the gun
        GunDistributionChat gunChat = new GunDistributionChat(this, woofs, villagers);
        Main.handleOutputMessage(channel, "Mafia is distributing the gun. Everyone muted meanwhile.");


        // - start the loop


    }

    @Override
    public boolean enoughPlayers(int signedUp) {
        return (getAmountOfPlayers() - signedUp) <= 0;
    }

    @Override
    public Map<String, Command> getGameCommands() {
        //TODO add game commands shoot, alive, history + more?
        return new HashMap<>();
    }


    @Override
    public void resetRolesAndPermissions() {
        //delete roles used by the game; the BOT_ROLE can stay
        Roles.deleteRole(channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);

        //reset permissions for @everyone in the game channel
        channel.getPermissionOverride(channel.getGuild().getPublicRole()).delete().complete();
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void handleCommand(CommandParser.CommandContainer cmd) {

    }
}

class PopcornListener extends ListenerAdapter implements CommandListener {

    private final Popcorn popcorn;
    private final static Logger log = LoggerFactory.getLogger(PopcornListener.class);


    PopcornListener(Popcorn p) {
        super();
        popcorn = p;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }

        //is this our channel? TODO private channels, for example the gun chat
        if (!popcorn.getChannel().getId().equals(event.getChannel().getId())) {
            return;
        }

        //does the message have our prefix?
        if (event.getMessage().getContent().startsWith(popcorn.getPREFIX())) {
            popcorn.handleCommand(CommandParser.parse(popcorn.getPREFIX(), event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        log.trace("PopcornListener on channel " + popcorn.getChannel().getName() + " started");
    }

    @Override
    public String getPrefix() {
        return popcorn.getPREFIX();
    }
}
