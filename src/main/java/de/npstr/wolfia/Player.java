package de.npstr.wolfia;

import de.npstr.wolfia.utils.DBWrapper;
import net.dv8tion.jda.managers.RoleManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by npstr on 23.08.2016
 */
public class Player {

    private static final Logger LOG = LogManager.getLogger();
    private static List<String> knownPlayerIds;

    private static DBWrapper db;

    static void setDB(DBWrapper db) {
        Player.db = db;
    }


    private String discordUserId;
    private String soloRoleId;
    private List<String> muNicks = new ArrayList<>();


    public static String asMention(String userId) {
        return "<@" + userId + ">";
    }

    public static String getDiscordNick(String userId) {
        return Main.jda.getUserById(userId).getUsername(); //TODO is calling this expensive?
    }

    private static Player loadPlayer(String discordUserId) {
        Player result = db.get(discordUserId, Player.class);
        if (result == null) {
            result = new Player(discordUserId);
            savePlayer(result);
        }
        return result;
    }

    private static void savePlayer(Player p) {
        db.set(p.discordUserId, p);
    }

    private static List<String> getKnownPlayerIds() {
        if (knownPlayerIds == null) {
            knownPlayerIds = db.get("knownPlayerIds", List.class);
            if (knownPlayerIds == null)
                knownPlayerIds = new ArrayList<>();

        }
        return knownPlayerIds;
    }

    private static void saveKnownPlayerIds() {
        if (knownPlayerIds == null) knownPlayerIds = getKnownPlayerIds();
        db.set("knownPlayerIds", knownPlayerIds);
    }

    private Player(String uId) {
        this.discordUserId = uId;
        singup = 0;
        savePlayer(this);
        getKnownPlayerIds().add(uId);
        saveKnownPlayerIds();

        //configuring the solo role of the new player
        RoleManager rm = Main.activeServer.createRole();
        soloRoleId = rm.getRole().getId();
        rm.setGrouped(false);
        rm.setMentionable(false);
        //rm.setName(discordUserId);
        rm.update();

        LOG.trace("New player " + this + " created");
    }

    public String getDiscordUserId() {
        return discordUserId;
    }

    public String asMention() {
        return asMention(discordUserId);
    }

    public String getDiscordNick() {
        return getDiscordNick(discordUserId);
    }

    private int singup;

    public static int getSingup(String userId) {
        return loadPlayer(userId).getSingup();
    }

    private int getSingup() {
        return singup;
    }

    public static int singup(String userId) {
        return loadPlayer(userId).singup();
    }

    private int singup() {
        singup++;
        savePlayer(this);
        return singup;
    }

    @Override
    public String toString() {
        String result = "Player discordID: " + discordUserId + ", muNicks: [";
        for (String s : muNicks) result += s + ", ";
        if (muNicks.size() > 0)
            result = result.substring(0, result.length() - 2);
        result += "]";
        return result;
    }

    @Override
    public int hashCode() {
        return discordUserId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != this.getClass()) return false;
        Player p = (Player) obj;

        return (this.discordUserId.equals(p.discordUserId));
    }
}
