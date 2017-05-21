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
import net.dv8tion.jda.core.utils.SimpleLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by napster on 27.04.17.
 * <p>
 * Redirects JDA logs to our own logging
 */
public class JDASimpleLogListener implements SimpleLog.LogListener {

    private static final Logger ourLog = LoggerFactory.getLogger(JDA.class);

    @Override
    public void onLog(final SimpleLog log, final SimpleLog.Level logLevel, final Object message) {
        if (logLevel == SimpleLog.Level.ALL) {
            ourLog.trace(message.toString());
        } else if (logLevel == SimpleLog.Level.TRACE) {
            ourLog.trace(message.toString());
        } else if (logLevel == SimpleLog.Level.DEBUG) {
            ourLog.debug(message.toString());
        } else if (logLevel == SimpleLog.Level.INFO) {
            ourLog.info(message.toString());
        } else if (logLevel == SimpleLog.Level.WARNING) {
            ourLog.warn(message.toString());
        } else if (logLevel == SimpleLog.Level.FATAL) {
            ourLog.error(message.toString());
        } else if (logLevel == SimpleLog.Level.OFF) {
            ourLog.trace(message.toString());
        }
    }

    @Override
    public void onError(final SimpleLog log, final Throwable err) {
        ourLog.error("", err);
    }
}
