package space.npstr.wolfia.utils;

import space.npstr.wolfia.Launcher;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PeriodicTimer {
    private final Consumer<Void> updateCallback;
    private final ScheduledFuture updates;
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
