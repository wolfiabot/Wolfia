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

import com.google.common.base.Suppliers;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.listings.Listings;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;
import java.util.List;
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
    private final ScheduledExecutorService jdaThreadPool;
    private final Listings listings;
    private final List<PrivateGuild> privateGuildListeners;
    private final GuildCachingListener guildCacheListener;
    private final UserMemberCachingListener userCacheListener;
    private final Supplier<ShardManager> singleton;


    public ShardManagerFactory(final WolfiaConfig wolfiaConfig, final CommandListener commandListener,
                               final OkHttpClient.Builder httpClientBuilder,
                               @Qualifier("jdaThreadPool") final ScheduledExecutorService jdaThreadPool, Listings listings,
                               List<PrivateGuild> privateGuildListeners, GuildCachingListener guildCacheListener,
                               UserMemberCachingListener userCacheListener) {

        this.wolfiaConfig = wolfiaConfig;
        this.commandListener = commandListener;
        this.httpClientBuilder = httpClientBuilder;
        this.jdaThreadPool = jdaThreadPool;
        this.listings = listings;
        this.privateGuildListeners = privateGuildListeners;
        this.guildCacheListener = guildCacheListener;
        this.userCacheListener = userCacheListener;
        this.singleton = Suppliers.memoize(this::createShardManager);
    }

    public ShardManager shardManager() {
        return singleton.get();
    }

    private ShardManager createShardManager() {
        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
                .setToken(this.wolfiaConfig.getDiscordToken())
                .setGame(Game.playing(App.GAME_STATUS))
                .addEventListeners(this.commandListener)
                .addEventListeners(userCacheListener)
                .addEventListeners(guildCacheListener)
                .addEventListeners(new InternalListener())
                .addEventListeners(this.listings)
                .addEventListeners(new WolfiaGuildListener())
                .addEventListeners(this.privateGuildListeners.toArray())
                .setHttpClientBuilder(this.httpClientBuilder
                        .eventListener(new OkHttpEventCounter("jda")))
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.GAME, CacheFlag.VOICE_STATE))
                .setEnableShutdownHook(false)
                .setRateLimitPool(this.jdaThreadPool, false)
                .setCallbackPool(this.jdaThreadPool, false)
                .setGatewayPool(this.jdaThreadPool, false)
                .setAudioEnabled(false);

        try {
            return builder.build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }
}
