package de.npstr.wolfia;

import java.util.Set;

/**
 * Created by npstr on 14.09.2016
 */
public interface Game {

    public int getAmountOfPlayers();

    public void start(Set<String> players);

    public boolean enoughPlayers(int signedUp);
}
