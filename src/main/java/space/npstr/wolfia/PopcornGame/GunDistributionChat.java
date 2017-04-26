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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.CommandHandler;
import space.npstr.wolfia.CommandListener;
import space.npstr.wolfia.Main;
import space.npstr.wolfia.PopcornGame.Commands.GiveGunCommand;
import space.npstr.wolfia.utils.CommandParser;
import space.npstr.wolfia.utils.Player;
import space.npstr.wolfia.utils.SimulatedGroupDMListener;

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
