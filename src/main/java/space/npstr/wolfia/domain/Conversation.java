/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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

package space.npstr.wolfia.domain;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.CheckReturnValue;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.system.EventWaiter;

/**
 * Base class for conversations. A conversation can be useful in many scenarios, especially to improve UX inside of
 * discord:
 * - no need for complex commands when we can incrementally collect all required information
 * - easy discovery of closely similar commands related to a feature
 */
public interface Conversation {

    /**
     * @return the event waiter to be used to wait for events.
     */
    EventWaiter getEventWaiter();

    /**
     * entry point to this conversation
     */
    boolean start(MessageContext context);

    /**
     * @return message sent when the event waiter times out.
     */
    default String getTimeoutMessage() {
        return "Canceled your last command.";
    }

    /**
     * @return timeout to be used when waiting for the next response from the user
     */
    default Duration getTimeout() {
        return Duration.ofMinutes(1);
    }

    @CheckReturnValue
    default boolean replyAndWaitForAnswer(MessageContext context, String sendMessage, Consumer<MessageReceivedEvent> action) {
        context.reply(sendMessage);
        getEventWaiter().waitForEvent(
                MessageReceivedEvent.class,
                waitForInvokerInChannel(context),
                action,
                getTimeout(),
                () -> context.replyWithMention(getTimeoutMessage())
        );
        return true;
    }

    @CheckReturnValue
    default Predicate<MessageReceivedEvent> waitForInvokerInChannel(MessageContext context) {
        return messageReceived -> messageReceived.getAuthor().equals(context.getInvoker())
                && messageReceived.getChannel().equals(context.getChannel());
    }
}
