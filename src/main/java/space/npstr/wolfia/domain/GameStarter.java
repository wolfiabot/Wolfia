/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

package space.npstr.wolfia.domain;

import net.dv8tion.jda.core.entities.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.ShutdownHandler;
import space.npstr.wolfia.commands.Context;
import space.npstr.wolfia.commands.debug.MaintenanceCommand;
import space.npstr.wolfia.commands.game.InCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.entities.Setup;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for loading the game setup and launching a new game
 */
@Component
public class GameStarter {

    private static final Logger log = LoggerFactory.getLogger(GameStarter.class);

    //needs to be synchronized so only one incoming command at a time can be in here
    public synchronized boolean startGame(Context context, Setup setup) throws IllegalGameStateException {

        long commandCallerId = context.getInvoker().getIdLong();
        final MessageChannel channel = context.getChannel();
        //need to synchronize on a class level due to this being an entity object that may be loaded twice from the database
        synchronized (Setup.class) {
            if (MaintenanceCommand.getMaintenanceFlag() || ShutdownHandler.isShuttingDown()) {
                RestActions.sendMessage(channel, "The bot is under maintenance. Please try starting a game later.");
                return false;
            }

            if (!setup.getInnedUsers().contains(commandCallerId)) {
                RestActions.sendMessage(channel, String.format("%s, only players that inned can start the game! Say `%s` to join!",
                        TextchatUtils.userAsMention(commandCallerId), WolfiaConfig.DEFAULT_PREFIX + InCommand.TRIGGER));
                return false;
            }

            //is there a game running already in this channel?
            if (Games.get(setup.getChannelId()) != null) {
                RestActions.sendMessage(channel, TextchatUtils.userAsMention(commandCallerId)
                        + ", there is already a game going on in this channel!");
                return false;
            }

            final Game game;
            try {
                game = setup.getGame().clazz.getConstructor().newInstance();
            } catch (final IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalGameStateException("Internal error, could not create the specified game.", e);
            }

            setup.cleanUpInnedPlayers(context.getJda().asBot().getShardManager());
            final Set<Long> inned = new HashSet<>(setup.getInnedUsers());
            if (!game.isAcceptablePlayerCount(inned.size(), setup.getMode())) {
                RestActions.sendMessage(channel, String.format(
                        "There aren't enough (or too many) players signed up! Please use `%s` for more information",
                        WolfiaConfig.DEFAULT_PREFIX + StatusCommand.TRIGGER));
                return false;
            }

            game.setDayLength(setup.getDayLengthMillis(), TimeUnit.MILLISECONDS);

            try {
                game.start(setup.getChannelId(), setup.getMode(), inned);
            } catch (final UserFriendlyException e) {
                log.info("Game start aborted due to user friendly exception", e);
                Games.remove(game);
                game.cleanUp();
                throw new UserFriendlyException(e.getMessage(), e);
            } catch (final Exception e) {
                //start failed with a fucked up exception
                Games.remove(game);
                game.cleanUp();
                throw new RuntimeException(String.format("%s, game start aborted due to:%n%s",
                        TextchatUtils.userAsMention(commandCallerId), e.getMessage()), e);
            }
            setup.clearInnedUsers();
            return true;
        }
    }
}
