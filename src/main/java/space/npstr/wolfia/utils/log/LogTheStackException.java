/*
 * Copyright (C) 2016-2025 the original author or authors
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

/**
 * This exception exists for the purpose of providing an anchor to the source of a call to queue()
 * NOTE: This does call the "expensive" {@link java.lang.Throwable#fillInStackTrace()} method like all instantiations
 * of exceptions do. There is no way around that if we want the original stack trace up to the queue() call in case of
 * an exception happening later.
 * <p>
 * Make sure to {@link java.lang.Throwable#initCause(Throwable)} on any instance that you create of these.
 */
public class LogTheStackException extends RuntimeException {

    public LogTheStackException() {
        super("Stack that lead to up to this:");
    }
}
