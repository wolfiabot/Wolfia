package de.npstr.wolfia.PopcornGame;

import de.npstr.wolfia.CommandHandler;
import de.npstr.wolfia.Game;
import de.npstr.wolfia.utils.CommandParser;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.PermissionOverride;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.managers.RoleManager;

import java.util.*;

/**
 * Created by npstr on 22.10.2016
 */
public class Popcorn implements Game, CommandHandler {

    private final TextChannel channel;

    public Popcorn(TextChannel channel) {
        this.channel = channel;
    }

    private void prepareChannel(Set<String> players) {

        // - read only access for everyone
        PermissionOverride po = channel.getOverrideForRole(channel.getGuild().getPublicRole());
        PermissionOverrideManager pom;
        if (po == null) pom = channel.createPermissionOverride(channel.getGuild().getPublicRole());
        else pom = po.getManager();

        pom.deny(Permission.MESSAGE_WRITE);
        pom.update();


        // - write permission for the players
        for (Role r : channel.getGuild().getRolesByName("Popcorn Player")) {
            r.getManager().delete();
        }
        RoleManager rm = channel.getGuild().createRole();
        rm.setName("Popcorn Player");
        for (String userId : players) {
            channel.getGuild().getManager().addRoleToUser(channel.getGuild().getUserById(userId), rm.getRole());
        }
        channel.getOverrideForRole(rm.getRole()).getManager().grant(Permission.MESSAGE_WRITE).update();

        //TODO
        // - revoke writing rights on the discord server for players during the game?
        // - remove other commands
        // - add player list command
        // - add shoot command
    }

    /**
     * @return amount of players this game needs to start
     */
    @Override
    public int getAmountOfPlayers() {
        return 11;
    }

    @Override
    public void start(Set<String> players) {
        prepareChannel(players);
        // - rand the roles
        List<String> rand = new ArrayList<>(players);
        Collections.shuffle(rand);
        Set<String> woofs = new HashSet<>(rand.subList(0, 4));//first 4 players on the shuffled list are the woofs
        Set<String> villagers = new HashSet<>(rand.subList(4, rand.size() - 1));

        //TODO
        //TODO bots aren't allowed to create group DMs which sucks; applying for white listing possible, but no JDA support
        //source: https://discordapp.com/developers/docs/resources/channel#group-dm-add-recipient
        //workaround: echo the messages of the players into their PMs
        // - rand the gun/let mafia vote the gun
        GunDistributionChat gunChat = new GunDistributionChat(this, woofs, villagers);


        // - start the loop


    }

    @Override
    public boolean enoughPlayers(int signedUp) {
        return (getAmountOfPlayers() - signedUp) > 0;
    }

    @Override
    public void handleCommand(CommandParser.CommandContainer cmd) {

    }
}
