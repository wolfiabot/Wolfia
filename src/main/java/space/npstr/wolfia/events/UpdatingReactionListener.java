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

package space.npstr.wolfia.events;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.sharding.ShardManager;

/**
 * This reaction listener will call for updates
 */
public class UpdatingReactionListener extends ReactionListener {

    private final Consumer<Void> updateCallback;
    private final ScheduledFuture<?> updates;

    /**
     * @param message              The message on which to listen for reactions
     * @param filter               filter by Members
     * @param reactionCallback     wat do when a reaction happens that went through the filter
     * @param selfDestructMillis   milliseconds after which this listener is removed and the message deleted
     * @param selfDestructCallback called on self destruct
     * @param updateMillis         interval for updates happening
     * @param updateCallback       called on update
     */
    public UpdatingReactionListener(ShardManager shardManager, ScheduledExecutorService executor, Message message,
                                    Predicate<Member> filter, Consumer<GenericMessageReactionEvent> reactionCallback,
                                    long selfDestructMillis, Consumer<Void> selfDestructCallback,
                                    long updateMillis, Consumer<Void> updateCallback) {
        super(shardManager, executor, message, filter, reactionCallback, selfDestructMillis, selfDestructCallback);

        this.updateCallback = updateCallback;
        this.updates = executor.scheduleAtFixedRate(this::update, updateMillis - 1000, updateMillis, TimeUnit.MILLISECONDS);
    }

    private void update() {
        this.updateCallback.accept(null);
    }

    @Override
    protected void destruct() {
        this.updates.cancel(true);
        super.destruct();
    }
}
