/*
 * Copyright (C) 2016-2023 the original author or authors
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

import java.util.HashSet;
import java.util.Set;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.ShutdownHandler;
import space.npstr.wolfia.commands.Context;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.domain.maintenance.MaintenanceService;
import space.npstr.wolfia.domain.settings.ChannelSettingsService;
import space.npstr.wolfia.domain.setup.GameSetup;
import space.npstr.wolfia.domain.setup.GameSetupService;
import space.npstr.wolfia.domain.setup.InCommand;
import space.npstr.wolfia.domain.setup.StatusCommand;
import space.npstr.wolfia.domain.setup.lastactive.ActivityService;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameResources;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import static java.util.Objects.requireNonNull;

/**
 * This class is responsible for loading the game setup and launching a new game
 */
@Component
public class GameStarter {

    private static final Logger log = LoggerFactory.getLogger(GameStarter.class);

    private final GameSetupService gameSetupService;
    private final MaintenanceService maintenanceService;
    private final GameRegistry gameRegistry;
    private final ActivityService activityService;
    private final ChannelSettingsService channelSettingsService;
    private final ShutdownHandler shutdownHandler;
    private final GameResources gameResources;

    public GameStarter(GameSetupService gameSetupService, MaintenanceService maintenanceService,
                       GameRegistry gameRegistry, ActivityService activityService,
                       ChannelSettingsService channelSettingsService, ShutdownHandler shutdownHandler,
                       GameResources gameResources) {

        this.gameSetupService = gameSetupService;
        this.maintenanceService = maintenanceService;
        this.gameRegistry = gameRegistry;
        this.activityService = activityService;
        this.channelSettingsService = channelSettingsService;
        this.shutdownHandler = shutdownHandler;
        this.gameResources = gameResources;
    }

    //needs to be synchronized so only one incoming command at a time can be in here
    public synchronized boolean startGame(Context context) throws IllegalGameStateException {

        long commandCallerId = context.getInvoker().getIdLong();
        MessageChannel channel = context.getChannel();

        if (this.maintenanceService.getMaintenanceFlag() || this.shutdownHandler.isShuttingDown()) {
            RestActions.sendMessage(channel, "The bot is under maintenance. Please try starting a game later.");
            return false;
        }

        GameSetupService.Action setupAction = this.gameSetupService.channel(context.getChannel().getIdLong());
        GameSetup setup = setupAction.getOrDefault();
        if (!setup.getInnedUsers().contains(commandCallerId)) {
            RestActions.sendMessage(channel, String.format("%s, only players that inned can start the game! Say `%s` to join!",
                    TextchatUtils.userAsMention(commandCallerId), WolfiaConfig.DEFAULT_PREFIX + InCommand.TRIGGER));
            return false;
        }

        //is there a game running already in this channel?
        if (this.gameRegistry.get(setup.getChannelId()) != null) {
            RestActions.sendMessage(channel, TextchatUtils.userAsMention(commandCallerId)
                    + ", there is already a game going on in this channel!");
            return false;
        }

        Game game;
        try {
            game = setup.getGame().constructor.apply(gameResources);
        } catch (Exception e) {
            log.error("Failed to invoke game constructor of {}", setup.getGame(), e);
            throw new IllegalGameStateException("Internal error, could not create the specified game.", e);
        }

        ShardManager shardManager = requireNonNull(context.getJda().getShardManager());
        boolean autoOut = this.channelSettingsService.channel(channel.getIdLong()).getOrDefault().isAutoOut();
        if (autoOut) {
            for (long userId : setup.getInnedUsers()) {
                boolean activeRecently = this.activityService.wasActiveRecently(userId);
                if (!activeRecently) {
                    setupAction.outUserDueToInactivity(userId, shardManager);
                }
            }
        }
        setup = setupAction.cleanUpInnedPlayers(shardManager);
        Set<Long> inned = new HashSet<>(setup.getInnedUsers());
        if (!game.isAcceptablePlayerCount(inned.size(), setup.getMode())) {
            RestActions.sendMessage(channel, String.format(
                    "There aren't enough (or too many) players signed up! Please use `%s` for more information",
                    WolfiaConfig.DEFAULT_PREFIX + StatusCommand.TRIGGER));
            return false;
        }

        game.setDayLength(setup.getDayLength());

        try {
            game.start(setup.getChannelId(), setup.getMode(), inned);
            this.gameRegistry.set(game);
        } catch (UserFriendlyException e) {
            log.info("Game start aborted due to user friendly exception");
            this.gameRegistry.remove(game);
            game.cleanUp();
            throw e;
        } catch (Exception e) {
            //start failed with a fucked up exception
            this.gameRegistry.remove(game);
            game.cleanUp();
            throw new RuntimeException(String.format("%s, game start aborted due to:%n%s",
                    TextchatUtils.userAsMention(commandCallerId), e.getMessage()), e);
        }
        setupAction.clearInnedUsers();
        return true;
    }
}
