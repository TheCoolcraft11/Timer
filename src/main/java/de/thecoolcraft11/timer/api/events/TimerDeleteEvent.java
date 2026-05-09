package de.thecoolcraft11.timer.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a timer is deleted.
 */
public class TimerDeleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String timerName;
    private final long finalTime;

    /**
     * Constructor for a timer delete event.
     *
     * @param timerName the name of the deleted timer
     * @param finalTime the final time of the timer before deletion
     */
    public TimerDeleteEvent(String timerName, long finalTime) {
        this.timerName = timerName;
        this.finalTime = finalTime;
    }

    /**
     * Get the name of the deleted timer.
     *
     * @return the timer name
     */
    public String getTimerName() {
        return timerName;
    }

    /**
     * Get the final time of the timer before it was deleted.
     *
     * @return the final time in seconds
     */
    public long getFinalTime() {
        return finalTime;
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
