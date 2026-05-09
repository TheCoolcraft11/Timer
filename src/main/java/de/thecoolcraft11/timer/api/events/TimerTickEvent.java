package de.thecoolcraft11.timer.api.events;

import de.thecoolcraft11.timer.TimerInstance;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a timer's time changes.
 * This event is fired every second when the timer is running.
 */
public class TimerTickEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final TimerInstance timer;
    private final boolean isGlobalTimer;
    private final long previousTime;
    private final long currentTime;

    /**
     * Constructor for a timer tick event.
     *
     * @param timer         the timer instance (null for global timer)
     * @param isGlobalTimer true if this is the global timer
     * @param previousTime  the previous time in seconds
     * @param currentTime   the current time in seconds
     */
    public TimerTickEvent(@Nullable TimerInstance timer, boolean isGlobalTimer, long previousTime, long currentTime) {
        this.timer = timer;
        this.isGlobalTimer = isGlobalTimer;
        this.previousTime = previousTime;
        this.currentTime = currentTime;
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
     * Get the previous time before this tick.
     *
     * @return the previous time in seconds
     */
    public long getPreviousTime() {
        return previousTime;
    }

    /**
     * Get the current time after this tick.
     *
     * @return the current time in seconds
     */
    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * Get the time difference (delta) between previous and current time.
     *
     * @return the time difference (positive for counting up, negative for counting down)
     */
    public long getTimeDelta() {
        return currentTime - previousTime;
    }

    /**
     * Check if the timer is counting up.
     *
     * @return true if counting up, false if counting down
     */
    public boolean isCountingUp() {
        return currentTime > previousTime;
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
