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

package space.npstr.wolfia.common;

import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

public class Exceptions {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Exceptions.class);

    public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER
            = (thread, throwable) -> log.error("Uncaught exception in thread {}", thread.getName(), throwable);

    public static <T> BiConsumer<T, Throwable> logIfFailed() {
        return (__, t) -> { if (t != null) log.error("Uncaught exception", t); };
    }

    /**
     * Unwrap completion exceptions and other useless stuff.
     */
    public static Throwable unwrap(Throwable throwable) {
        Throwable realCause = throwable;
        while ((realCause instanceof CompletionException) && realCause.getCause() != null) {
            realCause = realCause.getCause();
        }
        return realCause;
    }

    /**
     * Allows throwing checked exceptions as unchecked ones.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }

    private Exceptions() {
        //util class
    }
}
