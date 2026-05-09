package de.thecoolcraft11.timer.api.events;

import de.thecoolcraft11.timer.TimerInstance;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a timer is about to stop.
 * This event can be cancelled to prevent the timer from stopping.
 */
public class TimerStopEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final TimerInstance timer;
    private final boolean isGlobalTimer;
    private final StopReason reason;

    /**
     * Constructor for a timer stop event.
     *
     * @param timer         the timer instance (null for global timer)
     * @param isGlobalTimer true if this is the global timer
     * @param reason        the reason why the timer is stopping
     */
    public TimerStopEvent(@Nullable TimerInstance timer, boolean isGlobalTimer, StopReason reason) {
        this.timer = timer;
        this.isGlobalTimer = isGlobalTimer;
        this.reason = reason;
    }

    /**
     * Get the timer instance that is stopping.
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
     * Get the reason why the timer is stopping.
     *
     * @return the StopReason
     */
    public StopReason getReason() {
        return reason;
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

    /**
     * Enum representing the reason why a timer stopped.
     */
    public enum StopReason {
        /**
         * The timer was manually stopped by a command or API call
         */
        MANUAL,
        /**
         * The timer reached zero (countdown)
         */
        COUNTDOWN_COMPLETE,
        /**
         * The timer reached its maximum time
         */
        MAX_TIME_REACHED,
        /**
         * The timer was reset
         */
        RESET,
        /**
         * The timer was deleted
         */
        DELETED,
        /**
         * Unknown or other reason
         */
        OTHER
    }
}
