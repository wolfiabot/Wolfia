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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

/**
 * Created by napster on 28.04.17.
 * <p>
 * Write logback's status updates to a file.
 */
public class StatusToFileListener extends RollingFileAppender implements StatusListener {

    @Override
    public void addStatusEvent(Status status) {
        Level level;
        switch (status.getLevel()) {
            case Status.INFO:
                level = Level.INFO;
                break;
            case Status.WARN:
                level = Level.WARN;
                break;
            case Status.ERROR:
                level = Level.ERROR;
                break;
            default:
                level = Level.INFO;
        }
        LoggingEvent event = new LoggingEvent(StatusToFileListener.class.getSimpleName(),
                new LoggerContext().getLogger(StatusToFileListener.class),
                level,
                status.getMessage(),
                null,
                null);

        append(event);
    }
}
