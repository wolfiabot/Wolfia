package de.npstr.wolfia.PopcornGame;

import de.npstr.wolfia.CommandHandler;
import de.npstr.wolfia.CommandListener;
import de.npstr.wolfia.PopcornGame.Commands.GiveGunCommand;
import de.npstr.wolfia.pregame.Pregame;
import de.npstr.wolfia.utils.CommandParser;
import de.npstr.wolfia.utils.Player;
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

        String out2 = "Write here to talk to your mafia team. You have 5 minutes to decide who of the villagers gets" +
                "the gun.";

        String out3 = "Villagers: ";
        for (String s : villagePlayers) out3 += Player.getDiscordNick(s) + " ";

        String out4 = "Talk about it with your team, then type " + PREFIX + GiveGunCommand.COMMAND + "";
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);

        //does the message have our prefix?
        if (event.getMessage().getContent().startsWith(Pregame.PREFIX)) {
            commHand.handleCommand(CommandParser.parse(Pregame.PREFIX, event.getMessage().getContent().toLowerCase(), event));
        }
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
