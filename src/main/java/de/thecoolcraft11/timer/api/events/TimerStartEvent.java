package de.thecoolcraft11.timer.api.events;

import de.thecoolcraft11.timer.TimerInstance;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a timer is about to start.
 * This event can be cancelled to prevent the timer from starting.
 */
public class TimerStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final TimerInstance timer;
    private final boolean isGlobalTimer;

    /**
     * Constructor for a timer start event.
     *
     * @param timer         the timer instance (null for global timer)
     * @param isGlobalTimer true if this is the global timer
     */
    public TimerStartEvent(@Nullable TimerInstance timer, boolean isGlobalTimer) {
        this.timer = timer;
        this.isGlobalTimer = isGlobalTimer;
    }

    /**
     * Get the timer instance that is starting.
     *
     * @return the TimerInstance, or null if this is the global timer
     */
    @Nullable
    public TimerInstance getTimer() {
        return timer;
    }

    /**
     * Check if this event is for the global timer.
     *
     * @return true if this is the global timer, false otherwise
     */
    public boolean isGlobalTimer() {
        return isGlobalTimer;
    }

    /**
     * Get the timer name.
     *
     * @return the timer name, or "global" for the global timer
     */
    public String getTimerName() {
        return isGlobalTimer ? "global" : (timer != null ? timer.getName() : "unknown");
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
