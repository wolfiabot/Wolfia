/*
 * Copyright (C) 2016-2020 the original author or authors
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

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.system.DiscordEventListenerPublisher;
import space.npstr.wolfia.utils.Memoizer;

import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_REACTIONS;
import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGE_TYPING;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_EMOJIS;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MEMBERS;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGE_REACTIONS;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGE_TYPING;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.ACTIVITY;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.CLIENT_STATUS;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.EMOTE;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.MEMBER_OVERRIDES;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.VOICE_STATE;

/**
 * We need a factory around the ShardManager bean to proxy its dependencies, so we can have more realistic
 * setup of dependencies between the beans of the the application context in tests. Otherwise, our tests
 * do not ensure that we don't have circular dependencies in our application context.
 */
@Component
public class ShardManagerFactory {

    private static final List<GatewayIntent> GATEWAY_INTENTS = List.of(
            //GUILDS, not supported to be turned off by JDA, listed for documentation.
            GUILD_EMOJIS, // we use some custom ones, and usage will likely grow bigger in the future, so its a good idea to stay up to date with these
            GUILD_MESSAGES, // process messages in guilds
            DIRECT_MESSAGES, // we have some in-game roles that send their commands in DMs
            GUILD_MESSAGE_REACTIONS, // some reactions are used, for example when voting in the wolf chat
            DIRECT_MESSAGE_REACTIONS, // some in-game roles may also get reaction based selections in their DMs
            GUILD_MESSAGE_TYPING, DIRECT_MESSAGE_TYPING, // we use these to determine inactive users and remove them from our game queues
            // priviledged
            GUILD_MEMBERS // Required for our management of the wolfchat guilds
    );

    private final WolfiaConfig wolfiaConfig;
    private final DiscordEventListenerPublisher discordEventListenerPublisher;
    private final OkHttpClient.Builder httpClientBuilder;
    private final ScheduledExecutorService jdaThreadPool;
    private final Supplier<ShardManager> singleton;

    private volatile boolean created = false;


    public ShardManagerFactory(final WolfiaConfig wolfiaConfig, DiscordEventListenerPublisher discordEventListenerPublisher,
                               final OkHttpClient.Builder httpClientBuilder,
                               @Qualifier("jdaThreadPool") final ScheduledExecutorService jdaThreadPool) {

        this.wolfiaConfig = wolfiaConfig;
        this.discordEventListenerPublisher = discordEventListenerPublisher;
        this.httpClientBuilder = httpClientBuilder;
        this.jdaThreadPool = jdaThreadPool;
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
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(GATEWAY_INTENTS)
                .setToken(this.wolfiaConfig.getDiscordToken())
                .setActivity(Activity.playing(App.GAME_STATUS))
                .addEventListeners(this.discordEventListenerPublisher)
                .setHttpClientBuilder(this.httpClientBuilder
                        .eventListener(new OkHttpEventCounter("jda")))
                .disableCache(ACTIVITY, VOICE_STATE, EMOTE, CLIENT_STATUS)
                .enableCache(MEMBER_OVERRIDES)
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
