package de.thecoolcraft11.timer.api.events;

import de.thecoolcraft11.timer.TimerInstance;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a timer's time is about to be changed directly (set operation).
 * This event can be cancelled to prevent the time change.
 */
public class TimerTimeChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final TimerInstance timer;
    private final boolean isGlobalTimer;
    private final long oldTime;
    private long newTime;

    /**
     * Constructor for a timer time change event.
     *
     * @param timer         the timer instance (null for global timer)
     * @param isGlobalTimer true if this is the global timer
     * @param oldTime       the old time in seconds
     * @param newTime       the new time in seconds
     */
    public TimerTimeChangeEvent(@Nullable TimerInstance timer, boolean isGlobalTimer, long oldTime, long newTime) {
        this.timer = timer;
        this.isGlobalTimer = isGlobalTimer;
        this.oldTime = oldTime;
        this.newTime = newTime;
    }

    /**
     * Get the timer instance.
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

    /**
     * Get the old time before the change.
     *
     * @return the old time in seconds
     */
    public long getOldTime() {
        return oldTime;
    }

    /**
     * Get the new time that will be set.
     *
     * @return the new time in seconds
     */
    public long getNewTime() {
        return newTime;
    }

    /**
     * Set a different new time value.
     *
     * @param newTime the new time in seconds
     */
    public void setNewTime(long newTime) {
        this.newTime = newTime;
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
