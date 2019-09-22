package space.npstr.wolfia.config;

import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.wolfia.App;
import space.npstr.wolfia.common.Exceptions;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.entities.CachedGuild;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.discordwrapper.DiscordEntityProvider;
import space.npstr.wolfia.discordwrapper.JdaDiscordEntityProvider;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.listings.Listings;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class DiscordApiConfiguration {

    @Bean
    public DiscordEntityProvider jdaDiscordEntityProvider(ShardManager shardManager) {
        return new JdaDiscordEntityProvider(shardManager);
    }

    @Bean(destroyMethod = "")
    public ScheduledExecutorService jdaThreadPool(final ThreadPoolCollector threadPoolCollector) {
        AtomicInteger threadNumber = new AtomicInteger(0);
        ScheduledThreadPoolExecutor jdaThreadPool = new ScheduledThreadPoolExecutor(50, r -> {
            Thread thread = new Thread(r, "jda-pool-t" + threadNumber.getAndIncrement());
            thread.setUncaughtExceptionHandler(Exceptions.UNCAUGHT_EXCEPTION_HANDLER);
            return thread;
        });
        threadPoolCollector.addPool("jda", jdaThreadPool);
        return jdaThreadPool;
    }

    @Profile("!test")
    @Bean(destroyMethod = "") //we manage the lifecycle ourselves tyvm, see shutdown hook in the launcher
    public ShardManager shardManager(final WolfiaConfig wolfiaConfig, final Database database,
                                     final CommandListener commandListener, final OkHttpClient.Builder httpClientBuilder,
                                     final ScheduledExecutorService jdaThreadPool)
            throws LoginException {

        final DatabaseWrapper wrapper = database.getWrapper();
        return new DefaultShardManagerBuilder()
                .setToken(wolfiaConfig.getDiscordToken())
                .setGame(Game.playing(App.GAME_STATUS))
                .addEventListeners(commandListener)
                .addEventListeners(new UserMemberCachingListener<>(wrapper, CachedUser.class))
                .addEventListeners(new GuildCachingListener<>(wrapper, CachedGuild.class))
                .addEventListeners(new InternalListener())
                .addEventListeners(new Listings(httpClientBuilder))
                .addEventListeners(new WolfiaGuildListener())
                .setHttpClientBuilder(httpClientBuilder
                        .eventListener(new OkHttpEventCounter("jda")))
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.GAME, CacheFlag.VOICE_STATE))
                .setEnableShutdownHook(false)
                .setRateLimitPool(jdaThreadPool, false)
                .setCallbackPool(jdaThreadPool, false)
                .setGatewayPool(jdaThreadPool, false)
                .setAudioEnabled(false)
                .build();
    }
}
