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

package space.npstr.wolfia.listings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.OkHttpClient;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.wolfia.config.properties.ListingsConfig;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;

/**
 * Takes care of posting all our stats to various listing sites
 */
@Component
public class Listings {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Listings.class);

    //serves as both a set of registered listings and keeping track of ongoing tasks of posting stats
    private final Map<Listing, Future<?>> tasks = new HashMap<>();
    private final ExceptionLoggingExecutor executor;

    public Listings(OkHttpClient.Builder httpClientBuilder, ExceptionLoggingExecutor executor,
                    WolfiaConfig wolfiaConfig, ListingsConfig listingsConfig, ShardManager shardManager) {
        this.executor = executor;
        OkHttpClient httpClient = httpClientBuilder
                .eventListener(new OkHttpEventCounter("listings"))
                .build();
        this.tasks.put(new DiscordBotsPw(httpClient, wolfiaConfig, listingsConfig), null);
        this.tasks.put(new DiscordBotsOrg(httpClient, wolfiaConfig, listingsConfig), null);
        this.tasks.put(new Carbonitex(httpClient, wolfiaConfig, listingsConfig, shardManager), null);
    }

    private static boolean isTaskRunning(@Nullable Future<?> task) {
        return task != null && !task.isDone() && !task.isCancelled();
    }

    //according to discordbotspw and discordbotsorg docs: post stats on guild join, guild leave, and ready events
    private void postAllStats(JDA jda) {
        Set<Listing> listings = new HashSet<>(this.tasks.keySet());
        for (Listing listing : listings) {
            postStats(listing, jda);
        }
    }

    private synchronized void postStats(Listing listing, JDA jda) {
        Future<?> task = this.tasks.get(listing);
        if (isTaskRunning(task)) {
            log.info("Skipping posting stats to {} since there is a task to do that running already.", listing.name);
            return;
        }

        this.tasks.put(listing, this.executor.submit(() -> {
            try {
                listing.postStats(jda);
            } catch (InterruptedException e) {
                log.error("Task to send stats to {} interrupted", listing.name, e);
                Thread.currentThread().interrupt();
            }
        }));
    }


    @EventListener
    public void onGuildJoin(GuildJoinEvent event) {
        postAllStats(event.getJDA());
    }

    @EventListener
    public void onGuildLeave(GuildLeaveEvent event) {
        postAllStats(event.getJDA());
    }

    @EventListener
    public void onReady(ReadyEvent event) {
        postAllStats(event.getJDA());
    }
}
