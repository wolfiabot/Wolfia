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

package space.npstr.wolfia.game;

import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.utils.Action;
import space.npstr.wolfia.utils.TextchatUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Builds a game in a channel
 * Keeps track of sign ups and starts the game when everything is set up
 */
public class GameSetup {

    private final static Logger log = LoggerFactory.getLogger(Popcorn.class);

    //todo find a better place for this
    //true if a restart is planned, so games wont be able to be started
    public static boolean restartFlag = false;

    //internal values
    private final long channelId;

    //setup values with defaults
    private final Game game;
    private final Set<Long> innedPlayers = new HashSet<>();

    public GameSetup(final long channelId) {
        this.channelId = channelId;
        this.game = new Popcorn(channelId);
    }

    public long getChannelId() {
        return this.channelId;
    }

    public void inPlayer(final long userId, final Action success) {
        if (this.innedPlayers.contains(userId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s you have inned already.", TextchatUtils.userAsMention(userId));
        } else {
            this.innedPlayers.add(userId);
            success.action();
        }
    }

//    private void isPMable(final long userId, final Consumer<PrivateChannel> success) {
//        Wolfia.jda.getUserById(userId).openPrivateChannel().queue(success, exception -> {
//            Wolfia.handleOutputMessage(this.channelId, "%s please change your Privacy Settings on this server so I can PM you, or I won't be able to deliver your role PM", TextchatUtils.userAsMention(userId));
//        });
//    }

    public void outPlayer(final long userId) {
        this.innedPlayers.remove(userId);
    }

    public void startGame(final long commandCallerId) {

        if (!this.game.isAcceptablePlayerCount(this.innedPlayers.size())) {
            Wolfia.handleOutputMessage(this.channelId,
                    "There aren't enough (or too many) players signed up! Please use `%s%s` for more information",
                    Config.PREFIX, StatusCommand.COMMAND);
            return;
        }

        if (!this.innedPlayers.contains(commandCallerId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s: Only players that inned can start the game!", TextchatUtils.userAsMention(commandCallerId));
            return;
        }

        if (restartFlag) {
            //todo notify after done, save the setup state between restart etc
            Wolfia.handleOutputMessage(this.channelId, "The bot is getting ready to restart. Please try playing a game later.");
            return;
        }

        Setups.remove(this);
        Games.set(this.game);
        this.game.start(this.innedPlayers);
    }

    private void cleanUpInnedPlayers() {
        //did they leave the guild?
        final Set<Long> toBeOutted = new HashSet<>();
        final Guild g = Wolfia.jda.getTextChannelById(this.channelId).getGuild();
        this.innedPlayers.forEach(userId -> {
            if (g.getMemberById(userId) == null) {
                toBeOutted.add(userId);
            }
        });
        toBeOutted.forEach(this::outPlayer);

        //todo whenever time base ins are a thing, this is probably the place to check them
    }

    public String getStatus() {
        //clean up first
        cleanUpInnedPlayers();

        final StringBuilder sb = new StringBuilder(this.game.getStatus())
                .append("\nInned players (**")
                .append(this.innedPlayers.size())
                .append("**):");
        this.innedPlayers.forEach(userId -> {
            sb.append(" `").append(Wolfia.jda.getTextChannelById(this.channelId).getGuild().getMemberById(userId).getEffectiveName()).append("`,");
        });
        if (this.innedPlayers.size() > 0) sb.deleteCharAt(sb.length() - 1);//remove last comma
        return sb.toString();
    }
}
