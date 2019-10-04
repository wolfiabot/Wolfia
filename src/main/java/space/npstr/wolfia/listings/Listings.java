/*
 * Copyright (C) 2017 Dennis Neufeld
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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Created by napster on 23.07.17.
 * <p>
 * Takes care of posting all our stats to various listing sites
 */
@Component
public class Listings extends ListenerAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Listings.class);

    //serves as both a set of registered listings and keeping track of ongoing tasks of posting stats
    private final Map<Listing, Future> tasks = new HashMap<>();
    private final ExceptionLoggingExecutor executor;

    public Listings(OkHttpClient.Builder httpClientBuilder, ExceptionLoggingExecutor executor) {
        this.executor = executor;
        OkHttpClient httpClient = httpClientBuilder
                .eventListener(new OkHttpEventCounter("listings"))
                .build();
        this.tasks.put(new DiscordBotsPw(httpClient), null);
        this.tasks.put(new DiscordBotsOrg(httpClient), null);
        this.tasks.put(new Carbonitex(httpClient), null);
    }

    private static boolean isTaskRunning(@Nullable final Future task) {
        return task != null && !task.isDone() && !task.isCancelled();
    }

    //according to discordbotspw and discordbotsorg docs: post stats on guild join, guild leave, and ready events
    private void postAllStats(@Nonnull final JDA jda) {
        final Set<Listing> listings = new HashSet<>(this.tasks.keySet());
        for (final Listing listing : listings) {
            postStats(listing, jda);
        }
    }

    private synchronized void postStats(@Nonnull final Listing listing, @Nonnull final JDA jda) {
        final Future task = this.tasks.get(listing);
        if (isTaskRunning(task)) {
            log.info("Skipping posting stats to {} since there is a task to do that running already.", listing.name);
            return;
        }

        this.tasks.put(listing, this.executor.submit(() -> {
            try {
                listing.postStats(jda);
            } catch (final InterruptedException e) {
                log.error("Task to send stats to {} interrupted", listing.name, e);
                Thread.currentThread().interrupt();
            }
        }));
    }


    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        postAllStats(event.getJDA());
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        postAllStats(event.getJDA());
    }

    @Override
    public void onReady(final ReadyEvent event) {
        postAllStats(event.getJDA());
    }
}
