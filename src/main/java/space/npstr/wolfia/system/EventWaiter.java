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

package space.npstr.wolfia.system;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.dv8tion.jda.api.events.GenericEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Just like the JDA-Utils EventWaiter (Apache v2) but a bunch of improvements
 * - threadsafe
 * - doesn't block the main JDA threads
 * - efficient
 * - stricter types
 * - doesn't have to wait 2 years (and counting) for fixes
 */
@Component
public class EventWaiter {

    //this thread pool runs the actions as well as the timeout actions
    private final ScheduledExecutorService pool;
    //modifications to the hash map and sets have to go through this single threaded pool
    private final ScheduledExecutorService single;

    //These stateful collections are only threadsafe when modified though the single executor
    private final List<WaitingEvent<? extends GenericEvent>> toRemove = new ArrayList<>(); //reused object
    private final HashMap<Class<? extends GenericEvent>, Set<WaitingEvent<? extends GenericEvent>>> waitingEvents;

    public EventWaiter(ScheduledThreadPoolExecutor jdaThreadPool) {
        this.waitingEvents = new HashMap<>();
        this.pool = jdaThreadPool;
        this.single = new ScheduledThreadPoolExecutor(1);
    }

    public <T extends GenericEvent> EventWaiter.WaitingEvent<T> waitForEvent(
            Class<T> classType, Predicate<T> condition, Consumer<T> action, Duration timeout,
            Runnable timeoutAction
    ) {

        EventWaiter.WaitingEvent<T> we = new EventWaiter.WaitingEvent<>(condition, action);

        this.single.execute(() -> {
            this.waitingEvents.computeIfAbsent(classType, c -> new HashSet<>())
                    .add(we);
            this.single.schedule(() -> {
                var set = this.waitingEvents.get(classType);
                if (set == null) {
                    return;
                }
                if (set.remove(we)) {
                    this.pool.execute(timeoutAction);
                }

                if (set.isEmpty()) {
                    this.waitingEvents.remove(classType);
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        });
        return we;
    }

    @EventListener
    //avoids the same message that registers an event to be waited on also triggering the event waiter
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void onEvent(GenericEvent event) {
        Class<?> cc = event.getClass();

        // Runs at least once for the fired Event, at most
        // once for each superclass (excluding Object) because
        // Class#getSuperclass() returns null when the superclass
        // is primitive, void, or (in this case) Object.
        while (cc != null && cc != Object.class) {
            Class<?> clazz = cc;
            if (this.waitingEvents.get(clazz) != null) {
                this.single.execute(() -> {
                    Set<WaitingEvent<? extends GenericEvent>> set = this.waitingEvents.get(clazz);
                    @SuppressWarnings({"unchecked", "rawtypes"}) Predicate<WaitingEvent> filter = we -> we.attempt(event);
                    set.stream().filter(filter).forEach(this.toRemove::add);
                    set.removeAll(this.toRemove);
                    this.toRemove.clear();

                    if (set.isEmpty()) {
                        this.waitingEvents.remove(clazz);
                    }
                });
            }

            cc = cc.getSuperclass();
        }
    }

    public class WaitingEvent<T extends GenericEvent> {
        Predicate<T> condition;
        Consumer<T> action;

        WaitingEvent(Predicate<T> condition, Consumer<T> action) {
            this.condition = condition;
            this.action = action;
        }

        private boolean attempt(T event) {
            if (this.condition.test(event)) {
                EventWaiter.this.pool.execute(() -> this.action.accept(event));
                return true;
            }
            return false;
        }

        public void cancel() {
            EventWaiter.this.single.execute(
                    () -> EventWaiter.this.waitingEvents.values().forEach(set -> set.remove(this))
            );
        }
    }

}
