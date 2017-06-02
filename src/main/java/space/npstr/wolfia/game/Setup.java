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
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.utils.Action;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.TextchatUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Builds a game in a channel
 * Keeps track of sign ups and starts the game when everything is set up
 */
public class Setup {

    private final static Logger log = LoggerFactory.getLogger(Setup.class);

    //todo find a better place for this
    //true if a restart is planned, so games wont be able to be started
    public static boolean restartFlag = false;

    //internal values

    //hold all persistent values
    private final SetupEntity entity;


    private final Game game;

//    //transforms a data object to a real setup thing
//    public static Setup from(final SetupEntity setupEntity) {
//
//        Setup s = new Setup(setupEntity);
//
//        //todo improve this
//    }
//
//    //to be used in conjunction with the above static constructor from an entity object
//    private Setup(final SetupEntity setupEntity) {
//        this.entity = setupEntity;
//    }

    //create a fresh setup; default game is Popcorn, default mode is Wild
    public Setup(final long channelId) {
        this.entity = new SetupEntity();
        this.entity.setChannelId(channelId);
        this.entity.setGame(Games.GAME.POPCORN.name());
        this.entity.setMode(Popcorn.MODE.WILD.name());

        DbWrapper.merge(this.entity);

        this.game = Games.GAME.valueOf(this.entity.getGame()).createInstance();
        this.game.setMode(this.entity.getMode());
    }

    public void setGame(final String game) {
        try {
            final Games.GAME g = Games.GAME.valueOf(game);
            this.entity.setGame(g.name());
        } catch (final IllegalArgumentException e) {
            //todo UX output
        }
    }

    public void setMode(final String mode) {
        if (!this.game.getGameModes().contains(mode)) {
            //fail
        } else {
            this.game.setMode(mode);
        }
    }

    public long getChannelId() {
        return this.entity.getChannelId();
    }

    public void inPlayer(final long userId, final Action success) {
        if (this.entity.getInnedPlayers().contains(userId)) {
            Wolfia.handleOutputMessage(this.entity.getChannelId(), "%s, you have inned already.", TextchatUtils.userAsMention(userId));
        } else {
            this.entity.getInnedPlayers().add(userId);
            success.action();
            DbWrapper.merge(this.entity);
        }
    }

    public void outPlayer(final long userId) {
        this.entity.getInnedPlayers().remove(userId);
        DbWrapper.merge(this.entity);
    }

    public void startGame(final long commandCallerId) throws IllegalGameStateException {

        final Game game;
        try {
            game = Games.GAME.valueOf(this.entity.getGame()).getGameClass().newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalGameStateException("Internal error, could not create the specified game.");
        }

        if (!game.isAcceptablePlayerCount(this.entity.getInnedPlayers().size())) {
            Wolfia.handleOutputMessage(this.entity.getChannelId(),
                    "There aren't enough (or too many) players signed up! Please use `%s%s` for more information",
                    Config.PREFIX, StatusCommand.COMMAND);
            return;
        }

        if (!this.entity.getInnedPlayers().contains(commandCallerId)) {
            Wolfia.handleOutputMessage(this.entity.getChannelId(), "%s: Only players that inned can start the game!", TextchatUtils.userAsMention(commandCallerId));
            return;
        }

        if (restartFlag) {
            //todo notify after done, save the setup state between restart etc
            Wolfia.handleOutputMessage(this.entity.getChannelId(), "The bot is getting ready to restart. Please try playing a game later.");
            return;
        }

        Setups.remove(this);
        Games.set(game);
        game.start(this.entity.getChannelId(), this.entity.getInnedPlayers());
    }

    private void cleanUpInnedPlayers() {
        //did they leave the guild?
        final Set<Long> toBeOuted = new HashSet<>();
        final Guild g = Wolfia.jda.getTextChannelById(this.entity.getChannelId()).getGuild();
        this.entity.getInnedPlayers().forEach(userId -> {
            if (g.getMemberById(userId) == null) {
                toBeOuted.add(userId);
            }
        });
        toBeOuted.forEach(this::outPlayer);

        //todo whenever time base ins are a thing, this is probably the place to check them
    }

    public String getStatus() {
        //clean up first
        cleanUpInnedPlayers();

        final StringBuilder sb = new StringBuilder()
//                .append(game.getStatus()) todo fix this
                .append("\nInned players (**")
                .append(this.entity.getInnedPlayers().size())
                .append("**):");
        this.entity.getInnedPlayers().forEach(userId -> {
            sb.append(" `").append(Wolfia.jda.getTextChannelById(this.entity.getChannelId()).getGuild().getMemberById(userId).getEffectiveName()).append("`,");
        });
        if (this.entity.getInnedPlayers().size() > 0) sb.deleteCharAt(sb.length() - 1);//remove last comma
        return sb.toString();
    }
}
