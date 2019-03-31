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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.jda.listeners.DiscordEntityCacheUtil;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.db.entities.CachedGuild;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.discordwrapper.DiscordEntityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by napster on 07.12.17.
 */
public class SyncCommand extends BaseCommand implements IOwnerRestricted {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SyncCommand.class);

    private static final String ACTION_GUILDS = "guilds";
    private static final String ACTION_USERS = "users";

    private final ExecutorService syncService;
    private final DiscordEntityProvider discordEntityProvider;


    @Nonnull
    @Override
    protected String help() {
        return "Force a sync of the guilds and/or users present in the bot with the data saved in the database.";
    }


    public SyncCommand(DiscordEntityProvider discordEntityProvider, @Nonnull final String trigger, @Nonnull final String... aliases) {
        super(trigger, aliases);
        this.discordEntityProvider = discordEntityProvider;
        final int databasePoolSize = Launcher.getBotContext().getDatabase().getConnection().getMaxPoolSize();
        final int workers = Math.max(1, databasePoolSize / 2);//dont hog the database
        this.syncService = Executors.newFixedThreadPool(workers,
                runnable -> new Thread(runnable, "sync-command-worker"));
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) throws DatabaseException {

        boolean actionFound = false;
        if (context.msg.getContentRaw().toLowerCase().contains(ACTION_GUILDS)) {
            actionFound = true;
            context.reply("Starting guilds sync.");
            syncGuilds(
                    this.discordEntityProvider,
                    this.syncService,
                    Wolfia.getShards().stream().flatMap(jda -> jda.getGuildCache().stream()),
                    (duration, amount) -> context.reply(amount + " guilds synced in " + duration + "ms")
            );
        }

        if (context.msg.getContentRaw().toLowerCase().contains(ACTION_USERS)) {
            actionFound = true;
            context.reply("Starting users caching.");
            cacheUsers(
                    Wolfia.getShards(),
                    result -> context.reply("Shard " + result.shardId + ": "
                            + result.amount + " users cached in "
                            + result.duration + "ms")
            );
        }

        if (!actionFound) {
            context.reply("Did not find any action in your input, use one of these in your message:"
                    + "\n" + ACTION_GUILDS
                    + "\n" + ACTION_USERS
            );
            return false;
        }
        return true;
    }


    /**
     * @param executor
     *         Executor to run this task on
     * @param guilds
     *         The Guilds to be synced
     * @param resultConsumer
     *         Returns how long this took and how many guilds were processed
     */
    public static void syncGuilds(DiscordEntityProvider discordEntityProvider, @Nonnull final Executor executor,
                                  @Nonnull final Stream<Guild> guilds, @Nullable final BiConsumer<Long, Integer> resultConsumer) {

        final long started = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
        executor.execute(() -> {
            final Collection<DatabaseException> guildSyncDbExceptions = DiscordEntityCacheUtil.syncGuilds(
                    Launcher.getBotContext().getDatabase().getWrapper(),
                    guilds.peek(__ -> count.incrementAndGet()),
                    guildId -> discordEntityProvider.getGuildById(guildId).isPresent(),
                    CachedGuild.class
            );
            if (resultConsumer != null) {
                resultConsumer.accept(System.currentTimeMillis() - started, count.get());
            }
            if (!guildSyncDbExceptions.isEmpty()) {
                log.error("{} db exceptions thrown when syncing guild after start", guildSyncDbExceptions.size());
                for (final DatabaseException e : guildSyncDbExceptions) {
                    log.error("Db blew up when syncing guild", e);
                }
            }
        });
    }

    /**
     * @param shards
     *         the shards which users are to by cached
     * @param resultConsumer
     *         Returns how long which shard took and how many users/members were processed
     */
    public void cacheUsers(@Nonnull final Collection<JDA> shards, @Nullable final Consumer<SyncResult> resultConsumer) {
        for (final JDA jda : shards) {
            this.syncService.execute(() -> {
                final long started = System.currentTimeMillis();
                final AtomicInteger count = new AtomicInteger(0);
                log.info("Caching users for shard {} started", jda.getShardInfo().getShardId());
                //sync user cache
                final Collection<DatabaseException> userCacheDbExceptions = DiscordEntityCacheUtil.cacheAllMembers(
                        Launcher.getBotContext().getDatabase().getWrapper(),
                        jda.getGuildCache().stream()
                                .flatMap(guild -> guild.getMemberCache().stream())
                                .peek(__ -> count.incrementAndGet()),
                        CachedUser.class
                );
                if (!userCacheDbExceptions.isEmpty()) {
                    log.error("{} db exceptions thrown when caching users after start", userCacheDbExceptions.size());
                    for (final DatabaseException e : userCacheDbExceptions) {
                        log.error("Db blew up when caching user", e);
                    }
                }
                final int shardId = jda.getShardInfo().getShardId();
                final long duration = System.currentTimeMillis() - started;
                log.info("Caching users for shard {} done in {}ms", shardId, duration);
                if (resultConsumer != null) {
                    resultConsumer.accept(new SyncResult(shardId, duration, count.get()));
                }
            });
        }
    }

    public static final class SyncResult {
        public final int shardId;
        public final long duration;
        public final int amount;

        public SyncResult(final int shardId, final long duration, final int amount) {
            this.shardId = shardId;
            this.duration = duration;
            this.amount = amount;
        }
    }
}
