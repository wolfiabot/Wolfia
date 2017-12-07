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

package space.npstr.wolfia.game.exceptions;

/**
 * Created by napster on 21.05.17.
 * <p>
 * Exception for when a game enters a faulty state, the messages should be shown to the end user.
 */
public class IllegalGameStateException extends Exception {

    private static final long serialVersionUID = -3082580128565589439L;

    //package private to force creation with a concrete message
    IllegalGameStateException() {

    }

    public IllegalGameStateException(final String message) {
        super(message);
    }

    public IllegalGameStateException(final String message, final Throwable t) {
        super(message, t);
    }
}
