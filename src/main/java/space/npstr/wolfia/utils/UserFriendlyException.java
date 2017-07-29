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

package space.npstr.wolfia.utils;

/**
 * Created by napster on 22.06.17.
 * <p>
 * These Exceptions are allowed to happen, for example if the bot is misconfigured by the user,
 * and their content is save to be shown to users and considered part of the UX
 */
public class UserFriendlyException extends RuntimeException {

    private static final long serialVersionUID = 4569619083608513524L;

    //force creation with a message
    private UserFriendlyException() {

    }

    public UserFriendlyException(final String message, final Object... args) {
        super(String.format(message, args));
    }

    public UserFriendlyException(final String message) {
        super(message);
    }

    public UserFriendlyException(final String message, final Throwable t) {
        super(message, t);
    }
}
