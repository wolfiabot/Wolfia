package de.npstr.wolfia.game;

import net.dv8tion.jda.entities.User;

/**
 * Created by npstr on 23.08.2016
 */
public class Player {

    private User user;
    private String muNick = "guest";
    private Role role;

    public Player(User u) {
        this.user = u;
    }

    public User getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return user.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()) return false;
        Player p = (Player) obj;

        return (this.user.getId().equals(p.user.getId()));
    }
}
