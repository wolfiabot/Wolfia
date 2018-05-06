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

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 06.08.17.
 * <p>
 * This executor logs exceptions of its tasks.
 */
@Slf4j
public class ExceptionLoggingExecutor extends ScheduledThreadPoolExecutor {

    public ExceptionLoggingExecutor(final int threads, final String threadName) {
        this(threads, r -> new Thread(r, threadName));
    }

    public ExceptionLoggingExecutor(final int threads, final ThreadFactory threadFactory) {
        super(threads, threadFactory);
    }

    @Override
    public void execute(final Runnable command) {
        super.execute(wrapRunnableExceptionSafe(command));
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit timeUnit) {
        return super.schedule(wrapRunnableExceptionSafe(command), delay, timeUnit);
    }

    @Nonnull
    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return super.schedule(wrapExceptionSafe(callable), delay, unit);
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        return super.scheduleAtFixedRate(wrapRunnableExceptionSafe(command), initialDelay, period, unit);
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return super.scheduleWithFixedDelay(wrapRunnableExceptionSafe(command), initialDelay, delay, unit);
    }

    @Nonnull
    @Override
    public Future<?> submit(final Runnable task) {
        return super.submit(wrapRunnableExceptionSafe(task));
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return super.submit(wrapRunnableExceptionSafe(task), result);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return super.submit(wrapExceptionSafe(task));
    }

    public static Runnable wrapRunnableExceptionSafe(final Runnable runnable) {
        return wrapExceptionSafe(new ExceptionalTask(runnable));
    }

    public static Runnable wrapExceptionSafe(final ExceptionalRunnable runnable) {
        // scheduled executor services are sneaky bastards and will silently cancel tasks that throw an uncaught exception
        // related: http://code.nomad-labs.com/2011/12/09/mother-fk-the-scheduledexecutorservice
        // we don't really want our tasks to stop getting executed, and we want them to log any exceptions they encounter
        return () -> {
            try {
                runnable.run();
            } catch (final Throwable t) {
                log.error("Runnable encountered an exception: {}", t.getMessage(), t);
            }
        };
    }

    //returns null instead of throwing an exception and possibly canceling the task
    public static <V> Callable<V> wrapExceptionSafe(final Callable<V> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (final Throwable t) {
                log.error("Callable encountered an exception: {}", t.getMessage(), t);
                return null;
            }
        };
    }

    //these allow us to submit tasks with checked exceptions. bad practice? idk.
    @FunctionalInterface
    public interface ExceptionalRunnable {
        void run() throws Exception;
    }

    public static class ExceptionalTask implements ExceptionalRunnable {
        private final Runnable task;

        public ExceptionalTask(final Runnable task) {
            this.task = task;
        }

        @Override
        public void run() throws Exception {
            this.task.run();
        }
    }
}
