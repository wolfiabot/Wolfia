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

package space.npstr.wolfia.utils;

/**
 * These Exceptions are allowed to happen, for example if the bot is misconfigured by the user,
 * and their content is save to be shown to users and considered part of the UX
 */
public class UserFriendlyException extends RuntimeException {

    private static final long serialVersionUID = 4569619083608513524L;

    public UserFriendlyException(String message, Object... args) {
        super(String.format(message, args));
    }

    public UserFriendlyException(String message) {
        super(message);
    }

    public UserFriendlyException(String message, Throwable t) {
        super(message, t);
    }
}
