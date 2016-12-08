package de.npstr.wolfia;

import net.dv8tion.jda.entities.Channel;

import java.util.Map;
import java.util.Set;

/**
 * Created by npstr on 14.09.2016
 */
public interface Game {

    public int getAmountOfPlayers();

    public void start(Set<String> players);

    public boolean enoughPlayers(int signedUp);

    public Map<String, Command> getGameCommands();

    /**
     * this should revert each and everything the game touches in terms of discord roles and permissions to normal
     * most likely this includes deleting all discord roles used in the game and resetting @everyone permissions for the game channel
     */
    public void resetRolesAndPermissions();

    /**
     * @return Returns the main channel where the game is running
     */
    public Channel getChannel();
}
