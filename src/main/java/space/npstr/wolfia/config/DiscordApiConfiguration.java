package space.npstr.wolfia.config;

import net.dv8tion.jda.bot.sharding.ShardManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.wolfia.common.Exceptions;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.discordwrapper.DiscordEntityProvider;
import space.npstr.wolfia.discordwrapper.JdaDiscordEntityProvider;
import space.npstr.wolfia.events.PrivateGuildProvider;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class DiscordApiConfiguration {

    @Bean
    public DiscordEntityProvider jdaDiscordEntityProvider(ShardManager shardManager) {
        return new JdaDiscordEntityProvider(shardManager);
    }

    @Bean(destroyMethod = "", name = "jdaThreadPool")
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

    @Bean
    public List<PrivateGuild> privateGuildListeners(PrivateGuildProvider privateGuildProvider) {
        return privateGuildProvider.getAllGuilds();
    }

    @Profile("!test")
    @Bean(destroyMethod = "") //we manage the lifecycle ourselves tyvm, see shutdown hook in the launcher
    public ShardManager shardManager(ShardManagerFactory shardManagerFactory) {
        return shardManagerFactory.shardManager();
    }
}
