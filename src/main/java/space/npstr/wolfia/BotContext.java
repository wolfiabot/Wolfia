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

package space.npstr.wolfia;

import net.dv8tion.jda.bot.sharding.ShardManager;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.config.properties.ListingsConfig;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.domain.UserCache;
import space.npstr.wolfia.domain.room.PrivateRoomQueue;
import space.npstr.wolfia.domain.settings.ChannelSettingsService;
import space.npstr.wolfia.domain.stats.StatsRepository;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;

/**
 * Created by napster on 10.05.18.
 * <p>
 * Temporary uber class that allows resources that were previously accessed statically to continue to be accessed
 * that way through {@link Launcher#getBotContext()}, until the whole project is refactored into non-static components.
 * <p>
 * todo resolve this temporary file
 */
@Component
public class BotContext {

    private final Database database;
    private final WolfiaConfig wolfiaConfig;
    private final ListingsConfig listingsConfig;
    private final PrivateRoomQueue privateRoomQueue;
    private final ExceptionLoggingExecutor executor;
    private final ShardManager shardManager;
    private final ChannelSettingsService channelSettingsService;
    private final UserCache userCache;
    private final StatsRepository statsRepository;

    public BotContext(final Database database, final WolfiaConfig wolfiaConfig, final ListingsConfig listingsConfig,
                      PrivateRoomQueue privateRoomQueue, ExceptionLoggingExecutor executor,
                      ShardManager shardManager, ChannelSettingsService channelSettingsService, UserCache userCache,
                      StatsRepository statsRepository) {

        this.database = database;
        this.wolfiaConfig = wolfiaConfig;
        this.listingsConfig = listingsConfig;
        this.privateRoomQueue = privateRoomQueue;
        this.executor = executor;
        this.shardManager = shardManager;
        this.channelSettingsService = channelSettingsService;
        this.userCache = userCache;
        this.statsRepository = statsRepository;
    }

    public Database getDatabase() {
        return this.database;
    }

    public WolfiaConfig getWolfiaConfig() {
        return this.wolfiaConfig;
    }

    public ListingsConfig getListingsConfig() {
        return this.listingsConfig;
    }

    public PrivateRoomQueue getPrivateRoomQueue() {
        return this.privateRoomQueue;
    }

    public ExceptionLoggingExecutor getExecutor() {
        return this.executor;
    }

    public ShardManager getShardManager() {
        return this.shardManager;
    }

    public ChannelSettingsService getChannelSettingsService() {
        return this.channelSettingsService;
    }

    public UserCache getUserCache() {
        return this.userCache;
    }

    public StatsRepository getStatsRepository() {
        return this.statsRepository;
    }
}
