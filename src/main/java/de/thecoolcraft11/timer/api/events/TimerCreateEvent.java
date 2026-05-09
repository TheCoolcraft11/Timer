package de.thecoolcraft11.timer.api.events;

import de.thecoolcraft11.timer.MultiTimerManager;
import de.thecoolcraft11.timer.TimerInstance;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a new timer is created.
 */
public class TimerCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final TimerInstance timer;
    private final String timerName;
    private final MultiTimerManager.TimerType timerType;
    private final String targetId;

    /**
     * Constructor for a timer create event.
     *
     * @param timer     the newly created timer instance
     * @param timerName the name of the timer
     * @param timerType the type of timer
     * @param targetId  the target ID (for PLAYER/TEAM timers)
     */
    public TimerCreateEvent(TimerInstance timer, String timerName, MultiTimerManager.TimerType timerType, String targetId) {
        this.timer = timer;
        this.timerName = timerName;
        this.timerType = timerType;
        this.targetId = targetId;
    }

    /**
     * Get the newly created timer instance.
     *
     * @return the TimerInstance
     */
    @NotNull
    public TimerInstance getTimer() {
        return timer;
    }

    /**
     * Get the timer name.
     *
     * @return the timer name
     */
    public String getTimerName() {
        return timerName;
    }

    /**
     * Get the timer type.
     *
     * @return the TimerType
     */
    public MultiTimerManager.TimerType getTimerType() {
        return timerType;
    }

    /**
     * Get the target ID (for PLAYER/TEAM timers).
     *
     * @return the target ID, or null for GLOBAL timers
     */
    public String getTargetId() {
        return targetId;
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
