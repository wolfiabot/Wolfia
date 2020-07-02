/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import space.npstr.wolfia.Launcher;

public class PeriodicTimer {
    private final Consumer<Void> updateCallback;
    private final ScheduledFuture<?> updates;
    private final Consumer<Void> selfDestructCallback;

    /**
     * @param selfDestructMillis   milliseconds after which this listener is removed and the message deleted
     * @param selfDestructCallback called on self destruct
     * @param updateMillis         interval for updates happening
     * @param updateCallback       called on update
     */
    public PeriodicTimer(final long updateMillis, final Consumer<Void> updateCallback,
                         final long selfDestructMillis, final Consumer<Void> selfDestructCallback) {

        this.updateCallback = updateCallback;
        this.updates = Launcher.getBotContext().getExecutor().scheduleAtFixedRate(this::update, updateMillis - 1000, updateMillis, TimeUnit.MILLISECONDS);

        this.selfDestructCallback = selfDestructCallback;
        Launcher.getBotContext().getExecutor().schedule(this::destruct, selfDestructMillis, TimeUnit.MILLISECONDS);
    }

    private void update() {
        this.updateCallback.accept(null);
    }

    protected void destruct() {
        this.updates.cancel(true);
        this.selfDestructCallback.accept(null);
    }
}
