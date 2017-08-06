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

package space.npstr.wolfia.game.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 06.08.17.
 * <p>
 * This executor logs exceptions of its tasks.
 */
public class ExceptionLoggingExecutor {

    private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingExecutor.class);


    private final ScheduledThreadPoolExecutor scheduledExecutor;


    public ExceptionLoggingExecutor(final int threads, final String name) {
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(threads, r -> new Thread(r, name));
    }

    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit timeUnit) {
        final Runnable exceptionSafeTask = wrapExceptionSafe(task);
        return this.scheduledExecutor.scheduleAtFixedRate(exceptionSafeTask, initialDelay, period, timeUnit);
    }

    public ScheduledFuture<?> schedule(final Runnable task, final long delay, final TimeUnit timeUnit) {
        final Runnable exceptionSafeTask = wrapExceptionSafe(task);
        return this.scheduledExecutor.schedule(exceptionSafeTask, delay, timeUnit);
    }

    public Future<?> submit(final Runnable task) {
        final Runnable exceptionSafeTask = wrapExceptionSafe(task);
        return this.scheduledExecutor.submit(exceptionSafeTask);
    }

    public void execute(final Runnable task) {
        final Runnable exceptionSafeTask = wrapExceptionSafe(task);
        this.scheduledExecutor.execute(exceptionSafeTask);
    }

    public void shutdownNow() {
        this.scheduledExecutor.shutdownNow();
    }

    public void shutdown() {
        this.scheduledExecutor.shutdown();
    }

    public static Runnable wrapExceptionSafe(final Runnable task) {
        // scheduled executor services are sneaky bastards and will silently cancel tasks that throw an uncaught exception
        // related: http://code.nomad-labs.com/2011/12/09/mother-fk-the-scheduledexecutorservice
        // we don't really want our tasks to stop getting executed, and we want them to log any exceptions they encounter
        return () -> {
            try {
                task.run();
            } catch (final Throwable t) {
                log.error("Task encountered an exception: {}", t.getMessage(), t);
            }
        };
    }
}
