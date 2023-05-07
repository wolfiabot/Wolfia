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

package space.npstr.wolfia.game.tools;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This executor logs exceptions of its tasks.
 */
public class ExceptionLoggingExecutor extends ScheduledThreadPoolExecutor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExceptionLoggingExecutor.class);

    public ExceptionLoggingExecutor(int threads, String threadName) {
        this(threads, r -> new Thread(r, threadName));
    }

    public ExceptionLoggingExecutor(int threads, ThreadFactory threadFactory) {
        super(threads, threadFactory);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrapRunnableExceptionSafe(command));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit timeUnit) {
        return super.schedule(wrapRunnableExceptionSafe(command), delay, timeUnit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(wrapExceptionSafe(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(wrapRunnableExceptionSafe(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(wrapRunnableExceptionSafe(command), initialDelay, delay, unit);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(wrapRunnableExceptionSafe(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(wrapRunnableExceptionSafe(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(wrapExceptionSafe(task));
    }

    public static Runnable wrapRunnableExceptionSafe(Runnable runnable) {
        return wrapExceptionSafe(new ExceptionalTask(runnable));
    }

    public static Runnable wrapExceptionSafe(ExceptionalRunnable runnable) {
        // scheduled executor services are sneaky bastards and will silently cancel tasks that throw an uncaught exception
        // related: http://code.nomad-labs.com/2011/12/09/mother-fk-the-scheduledexecutorservice
        // we don't really want our tasks to stop getting executed, and we want them to log any exceptions they encounter
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Runnable encountered an exception: {}", e.getMessage(), e);
            }
        };
    }

    //returns null instead of throwing an exception and possibly canceling the task
    public static <V> Callable<V> wrapExceptionSafe(Callable<V> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                log.error("Callable encountered an exception: {}", e.getMessage(), e);
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

        public ExceptionalTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() throws Exception {
            this.task.run();
        }
    }
}
