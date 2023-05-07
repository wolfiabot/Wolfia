/*
 * Copyright (C) 2016-2020 the original author or authors
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;

/**
 * A self destructing listener for reactions to a single message
 */
public class ReactionListener extends ListenerAdapter {

    private final ShardManager shardManager;
    private final long messageId;
    private final Predicate<Member> filter;
    private final Consumer<GenericMessageReactionEvent> callback;
    private final Consumer<Void> selfDestructCallback;

    /**
     * @param message              The message on which to listen for reactions
     * @param filter               filter by Members
     * @param callback             wat do when a reaction happens that went through the filter
     * @param selfDestructMillis   milliseconds after which this listener is removed and the message deleted
     * @param selfDestructCallback called on self destruct
     */
    public ReactionListener(ShardManager shardManager, ScheduledExecutorService executor, Message message, Predicate<Member> filter, Consumer<GenericMessageReactionEvent> callback,
                            long selfDestructMillis, Consumer<Void> selfDestructCallback) {

        this.shardManager = shardManager;
        this.messageId = message.getIdLong();
        this.filter = filter;
        this.callback = callback;
        this.selfDestructCallback = selfDestructCallback;

        executor.schedule(this::destruct, selfDestructMillis, TimeUnit.MILLISECONDS);
    }

    protected void destruct() {
        //remove the listener
        this.shardManager.removeEventListener(this);
        this.selfDestructCallback.accept(null);
    }

    @Override
    public void onGenericMessageReaction(GenericMessageReactionEvent event) {
        if (this.messageId != event.getMessageIdLong()) {
            return;
        }

        if (!this.filter.test(event.getMember())) {
            return;
        }

        this.callback.accept(event);
    }
}

