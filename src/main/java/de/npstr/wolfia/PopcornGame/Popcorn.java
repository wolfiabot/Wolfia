package de.npstr.wolfia.PopcornGame;

import de.npstr.wolfia.*;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.Roles;
import de.npstr.wolfia.utils.Sneaky;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.exceptions.PermissionException;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by npstr on 22.10.2016
 */
public class Popcorn implements Game, CommandHandler {

    private final TextChannel channel;

    private final static Logger LOG = LogManager.getLogger();

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
        channel.getGuild().getManager().addRoleToUser(channel.getGuild().getUserById(Main.jda.getSelfInfo().getId()), botRole).update();
        Roles.grant(channel, botRole, Permission.MESSAGE_WRITE, true);


        // - read only access for @everyone in the game channel
        Roles.grant(channel, channel.getGuild().getPublicRole(), Permission.MESSAGE_WRITE, false);


        // - write permission for the players
        Roles.deleteRole(channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
        Role playerRole = Roles.getOrCreateRole(channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
        Roles.grant(channel, playerRole, Permission.MESSAGE_WRITE, true);

        for (String userId : players) {
            channel.getGuild().getManager().addRoleToUser(channel.getGuild().getUserById(userId), playerRole);
        }
        channel.getGuild().getManager().update();


        // - revoke writing rights on the discord server for players during the game?
        playerRole.getManager().revoke(Permission.MESSAGE_WRITE).update();

        //TODO
        // - add player list command
        // - add shoot command
    }


    /**
     * @return amount of players this game needs to start
     */
    @Override
    public int getAmountOfPlayers() {
        if (Sneaky.DEBUG) return 1;
        else return 11;
    }

    @Override
    public void start(Set<String> players) {
        try {
            prepareChannel(players);
        } catch (PermissionException e) {
            LOG.warn("Could not prepare channel " + channel.getName() + ", id: " + channel.getId() + ", due to missing" +
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
        if (Sneaky.DEBUG) {
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
        channel.getOverrideForRole(channel.getGuild().getPublicRole()).getManager().delete();
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
    private final static Logger LOG = LogManager.getLogger();


    PopcornListener(Popcorn p) {
        super();
        popcorn = p;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        //bot should ignore itself
        if (event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) {
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
        LOG.trace("PopcornListener on channel " + popcorn.getChannel().getName() + " started");
    }

    @Override
    public String getPrefix() {
        return popcorn.getPREFIX();
    }
}
