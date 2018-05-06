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

package space.npstr.wolfia.utils.log;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.utils.discord.RestActions;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by napster on 28.07.17.
 * <p>
 * Logs some bot-wide events into a discord channel of our choice
 * Avoid spamming the events as this gets ratelimited easily (5/5s)
 * Singleton pattern cause I only need one of them, there is not technical reason for it though.
 */
@Slf4j
public class DiscordLogger {

    private static final ScheduledExecutorService x = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "discord-logger-executor"));
    private static DiscordLogger discordLogger;

    //singleton approach
    public synchronized static DiscordLogger getLogger() {
        if (discordLogger == null) {
            discordLogger = new DiscordLogger();
        }
        return discordLogger;
    }

    public synchronized static void shutdown(final long timeout, final TimeUnit timeUnit) {
        if (discordLogger != null) {
            x.shutdown();
            try {
                x.awaitTermination(timeout, timeUnit);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private DiscordLogger() {
    }

    private void sendMessage(final String message) {
        try {
            while (!Wolfia.isStarted()) {
                Thread.sleep(1000);
            }

            TextChannel channel = Wolfia.getTextChannelById(Config.C.logChannelId);
            while (channel == null) {
                Thread.sleep(1000);
                channel = Wolfia.getTextChannelById(Config.C.logChannelId);
            }

            final Consumer<Message> onSuccess = ignored -> log.info(message);//log into file
            final Consumer<Throwable> onFail = t -> {
                log.error("Exception when sending discord logger message", t);
                log(message);//readd it to the queue
            };
            RestActions.sendMessage(channel, message, onSuccess, onFail);

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Future log(final String message) {
        return x.schedule(() -> sendMessage(message), 0, TimeUnit.NANOSECONDS);
    }

    public Future log(final String message, final Object... objects) {
        return log(String.format(message, objects));
    }

}
