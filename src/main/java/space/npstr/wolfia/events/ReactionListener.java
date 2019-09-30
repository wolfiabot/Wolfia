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

package space.npstr.wolfia.events;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.Launcher;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by napster on 18.06.17.
 * <p>
 * A self destructing listener for reactions to a single message
 */
public class ReactionListener extends ListenerAdapter {

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
    public ReactionListener(final Message message, final Predicate<Member> filter, final Consumer<GenericMessageReactionEvent> callback,
                            final long selfDestructMillis, final Consumer<Void> selfDestructCallback) {
        this.messageId = message.getIdLong();
        this.filter = filter;
        this.callback = callback;
        this.selfDestructCallback = selfDestructCallback;

        Launcher.getBotContext().getExecutor().schedule(this::destruct, selfDestructMillis, TimeUnit.MILLISECONDS);
    }

    protected void destruct() {
        //remove the listener
        Launcher.getBotContext().getShardManager().removeEventListener(this);
        this.selfDestructCallback.accept(null);
    }

    @Override
    public void onGenericMessageReaction(final GenericMessageReactionEvent event) {
        if (this.messageId != event.getMessageIdLong()) {
            return;
        }

        if (!this.filter.test(event.getMember())) {
            return;
        }

        this.callback.accept(event);
    }
}

