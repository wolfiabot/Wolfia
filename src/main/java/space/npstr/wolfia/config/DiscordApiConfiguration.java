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

import net.dv8tion.jda.bot.sharding.ShardManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.wolfia.common.Exceptions;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.entities.CachedGuild;
import space.npstr.wolfia.db.entities.CachedUser;
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

    @Bean
    public GuildCachingListener<CachedGuild> guildCachingListener(Database database) {
        return new GuildCachingListener<>(database.getWrapper(), CachedGuild.class);
    }

    @Bean
    public UserMemberCachingListener<CachedUser> userMemberCachingListener(Database database) {
        return new UserMemberCachingListener<>(database.getWrapper(), CachedUser.class);
    }

    @Profile("!test")
    @Bean(destroyMethod = "") //we manage the lifecycle ourselves tyvm, see shutdown hook in the launcher
    public ShardManager shardManager(ShardManagerFactory shardManagerFactory) {
        return shardManagerFactory.shardManager();
    }
}
