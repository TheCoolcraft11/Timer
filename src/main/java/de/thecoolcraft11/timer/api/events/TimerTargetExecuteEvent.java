package de.thecoolcraft11.timer.api.events;

import de.thecoolcraft11.timer.TimerInstance;
import de.thecoolcraft11.timer.TimerTarget;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a timer target is about to be executed.
 * This event can be cancelled to prevent the target from executing.
 */
public class TimerTargetExecuteEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final TimerInstance timer;
    private final boolean isGlobalTimer;
    private final TimerTarget target;
    private final long currentTime;

    /**
     * Constructor for a timer target execute event.
     *
     * @param timer         the timer instance (null for global timer)
     * @param isGlobalTimer true if this is the global timer
     * @param target        the target being executed
     * @param currentTime   the current time when the target executes
     */
    public TimerTargetExecuteEvent(@Nullable TimerInstance timer, boolean isGlobalTimer, TimerTarget target, long currentTime) {
        this.timer = timer;
        this.isGlobalTimer = isGlobalTimer;
        this.target = target;
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
     * Get the target being executed.
     *
     * @return the TimerTarget
     */
    @NotNull
    public TimerTarget getTarget() {
        return target;
    }

    /**
     * Get the target ID.
     *
     * @return the target identifier
     */
    public String getTargetId() {
        return target.getId();
    }

    /**
     * Get the target time.
     *
     * @return the time in seconds when the target should execute
     */
    public long getTargetTime() {
        return target.getTime();
    }

    /**
     * Get the command associated with this target.
     *
     * @return the command, or null if not a command target
     */
    @Nullable
    public String getCommand() {
        return target.getCommand();
    }

    /**
     * Get the current timer time when this target is executing.
     *
     * @return the current time in seconds
     */
    public long getCurrentTime() {
        return currentTime;
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
