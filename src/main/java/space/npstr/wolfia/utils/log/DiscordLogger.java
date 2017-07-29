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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by napster on 28.07.17.
 * <p>
 * Logs some bot-wide events into a discord channel of our choice
 * Avoid spamming the events as this gets ratelimited easily (5/5s)
 */
public class DiscordLogger {

    private static final Logger log = LoggerFactory.getLogger(DiscordLogger.class);

    private static DiscordLogger discordLogger;

    //singleton approach
    public synchronized static DiscordLogger getLogger() {
        if (discordLogger == null) {
            discordLogger = new DiscordLogger();
        }
        return discordLogger;
    }

    public synchronized static void shutdown() {
        if (discordLogger != null) {
            discordLogger.shutdown = true;
            discordLogger.task.cancel(true);
        }
    }


    private final LinkedBlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private boolean shutdown = false;
    private final Future task;

    private DiscordLogger() {
        //schedule this in the long running executor
        this.task = Wolfia.schedule(this::sendMessagesLoop, 0, TimeUnit.NANOSECONDS);
    }

    private boolean errorSent = false;

    private void sendMessagesLoop() {

        while (!this.shutdown && !Thread.interrupted()) {

            //throw an error if the queue grows too big
            if (this.messageQueue.size() >= 1000 && !this.errorSent) {
                log.error("Discord logger queue reached a size of 1000 messages. This is not expected behaviour.");
                this.errorSent = true;
            } else if (this.messageQueue.size() < 1000 && this.errorSent) {
                this.errorSent = false; //queue recovered, allow throwing an error again
            }

            //log the queue size above a certain threshold
            if (this.messageQueue.size() > 10) {
                log.info("Discord logger queue size: {}", this.messageQueue.size());
            }

            try {
                final String message = this.messageQueue.take();

                JDA jda = Wolfia.jda;
                while (jda == null) {
                    Thread.sleep(1000);
                    jda = Wolfia.jda;
                }

                TextChannel channel = jda.getTextChannelById(Config.C.logChannelId);
                while (channel == null) {
                    Thread.sleep(1000);
                    channel = jda.getTextChannelById(Config.C.logChannelId);
                }

                final Consumer<Message> onSuccess = ignored -> log.info(message);//log into file
                final Consumer<Throwable> onFail = t -> {
                    log.error("Exception when sending discord logger message", t);
                    this.messageQueue.add(message); //readd it to the queue
                };
                Wolfia.handleOutputMessage(channel, onSuccess, onFail, message);

            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }

    }

    public void log(final String message) {
        this.messageQueue.add(message);
    }

    public void log(final String message, final Object... objects) {
        log(String.format(message, objects));
    }

}
