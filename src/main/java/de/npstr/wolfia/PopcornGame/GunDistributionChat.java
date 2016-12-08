package de.npstr.wolfia.PopcornGame;

import de.npstr.wolfia.CommandHandler;
import de.npstr.wolfia.CommandListener;
import de.npstr.wolfia.Main;
import de.npstr.wolfia.PopcornGame.Commands.GiveGunCommand;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.Player;
import de.npstr.wolfia.utils.SimulatedGroupDMListener;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

import java.util.Set;

/**
 * Created by npstr on 05.11.2016
 */
public class GunDistributionChat extends SimulatedGroupDMListener implements CommandListener {

    private final String PREFIX = "!";

    private final CommandHandler commHand;

    /**
     * @param mafiaPlayers the woofs that decide upon who gets the gun
     */
    public GunDistributionChat(CommandHandler commHand, Set<String> mafiaPlayers, Set<String> villagePlayers) {
        super(mafiaPlayers);
        this.commHand = commHand;

        String out = "Mafia members: ";
        for (String s : mafiaPlayers) out += Player.getDiscordNick(s) + " ";

        out += "\nWrite here to talk to your mafia team. You have 5 minutes to decide who of the villagers gets " +
                "the gun.";

        out += "\nVillagers: ";
        for (String s : villagePlayers) out += Player.getDiscordNick(s) + " ";

        out += "\nTalk about it with your team, then use the " + GiveGunCommand.COMMAND + " command to give a gun.";

        for (String s : mafiaPlayers) {
            Main.handleOutputMessage(s, out);
        }
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);

        //does the message have our prefix?
        if (event.getMessage().getContent().startsWith(getPrefix())) {
            commHand.handleCommand(CommandParser.parse(getPrefix(), event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
