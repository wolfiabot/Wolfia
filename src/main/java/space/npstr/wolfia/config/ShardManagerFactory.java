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

package space.npstr.wolfia.config;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.room.PrivateRoomQueue;
import space.npstr.wolfia.domain.settings.GuildCacheListener;
import space.npstr.wolfia.domain.setup.lastactive.DiscordActivityListener;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.listings.Listings;
import space.npstr.wolfia.utils.Memoizer;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * We need a factory around the ShardManager bean to proxy its dependencies, so we can have more realistic
 * setup of dependencies between the beans of the the application context in tests. Otherwise, our tests
 * do not ensure that we don't have circular dependencies in our application context.
 */
@Component
public class ShardManagerFactory {

    private final WolfiaConfig wolfiaConfig;
    private final CommandListener commandListener;
    private final OkHttpClient.Builder httpClientBuilder;
    private final InternalListener internalListener;
    private final ScheduledExecutorService jdaThreadPool;
    private final Listings listings;
    private final PrivateRoomQueue privateRoomQueue;
    private final GuildCacheListener guildCacheListener;
    private final DiscordActivityListener activityListener;
    private final Supplier<ShardManager> singleton;

    private volatile boolean created = false;


    public ShardManagerFactory(final WolfiaConfig wolfiaConfig, final CommandListener commandListener,
                               final OkHttpClient.Builder httpClientBuilder, InternalListener internalListener,
                               @Qualifier("jdaThreadPool") final ScheduledExecutorService jdaThreadPool, Listings listings,
                               PrivateRoomQueue privateRoomQueue, GuildCacheListener guildCacheListener,
                               DiscordActivityListener activityListener) {

        this.wolfiaConfig = wolfiaConfig;
        this.commandListener = commandListener;
        this.httpClientBuilder = httpClientBuilder;
        this.internalListener = internalListener;
        this.jdaThreadPool = jdaThreadPool;
        this.listings = listings;
        this.privateRoomQueue = privateRoomQueue;
        this.guildCacheListener = guildCacheListener;
        this.activityListener = activityListener;
        this.singleton = Memoizer.memoize(this::createShardManager);
    }

    public ShardManager shardManager() {
        return singleton.get();
    }

    public synchronized void shutdown() {
        if (this.created) {
            this.singleton.get().shutdown();
        }
    }

    private synchronized ShardManager createShardManager() {
        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
                .setToken(this.wolfiaConfig.getDiscordToken())
                .setActivity(Activity.playing(App.GAME_STATUS))
                .addEventListeners(this.activityListener)
                .addEventListeners(this.commandListener)
                .addEventListeners(this.guildCacheListener)
                .addEventListeners(this.internalListener)
                .addEventListeners(this.listings)
                .addEventListeners(new WolfiaGuildListener())
                .addEventListeners(this.privateRoomQueue.getAllManagedRooms().toArray())
                .setHttpClientBuilder(this.httpClientBuilder
                        .eventListener(new OkHttpEventCounter("jda")))
                .setEnabledCacheFlags(EnumSet.noneOf(CacheFlag.class))
                .setEnableShutdownHook(false)
                .setRateLimitPool(this.jdaThreadPool, false)
                .setCallbackPool(this.jdaThreadPool, false)
                .setGatewayPool(this.jdaThreadPool, false);

        ShardManager shardManager;
        try {
            shardManager = builder.build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }

        this.created = true;
        return shardManager;
    }
}
