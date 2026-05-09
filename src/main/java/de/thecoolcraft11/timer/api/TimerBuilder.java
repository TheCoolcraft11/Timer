package de.thecoolcraft11.timer.api;

import de.thecoolcraft11.timer.AnimationType;
import de.thecoolcraft11.timer.MultiTimerManager;
import de.thecoolcraft11.timer.Timer;
import de.thecoolcraft11.timer.TimerInstance;

/**
 * Builder class for creating new timers with a fluent API.
 *
 * <p>Example usage:
 * <pre>
 * api.createTimer()
 *    .name("myTimer")
 *    .type(TimerType.GLOBAL)
 *    .countingUp(true)
 *    .visible(true)
 *    .showName(true)
 *    .animationType("gradient")
 *    .color1("#FF0000")
 *    .color2("#0000FF")
 *    .animationSpeed(1.5)
 *    .maxTime(3600)
 *    .showMaxTime(true)
 *    .build();
 * </pre>
 */
public class TimerBuilder {
    private final TimerAPI api;

    private String name;
    private MultiTimerManager.TimerType type = MultiTimerManager.TimerType.GLOBAL;
    private String targetId = null;
    private boolean countingUp = true;
    private long initialTime = 0;
    private boolean startRunning = false;
    private boolean visible = true;
    private boolean showName = true;


    private AnimationType animationType = AnimationType.GRADIENT;
    private String color1 = "#00FF00";
    private String color2 = "#0080FF";
    private double animationSpeed = 1.0;
    private int animationDurationTicks = 10;


    private long maxTime = 0;
    private boolean showMaxTime = false;
    private String maxTargetCommand = null;

    public TimerBuilder(TimerAPI api, Timer plugin) {
        this.api = api;
    }

    /**
     * Set the name of the timer (required).
     *
     * @param name the timer name
     * @return this builder
     */
    public TimerBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the type of timer.
     *
     * @param type the TimerType (GLOBAL, PLAYER, or TEAM)
     * @return this builder
     */
    public TimerBuilder type(MultiTimerManager.TimerType type) {
        this.type = type;
        return this;
    }

    /**
     * Set the target ID for PLAYER or TEAM timers.
     * For PLAYER timers, this should be the player's UUID.
     * For TEAM timers, this should be the team name.
     *
     * @param targetId the target identifier
     * @return this builder
     */
    public TimerBuilder targetId(String targetId) {
        this.targetId = targetId;
        return this;
    }

    /**
     * Set whether the timer counts up or down.
     *
     * @param countingUp true for counting up, false for counting down
     * @return this builder
     */
    public TimerBuilder countingUp(boolean countingUp) {
        this.countingUp = countingUp;
        return this;
    }

    /**
     * Set the initial time of the timer in seconds.
     *
     * @param seconds the initial time
     * @return this builder
     */
    public TimerBuilder initialTime(long seconds) {
        this.initialTime = seconds;
        return this;
    }

    /**
     * Set whether the timer should start running immediately.
     *
     * @param startRunning true to start running, false otherwise
     * @return this builder
     */
    public TimerBuilder startRunning(boolean startRunning) {
        this.startRunning = startRunning;
        return this;
    }

    /**
     * Set whether the timer is visible to players.
     *
     * @param visible true to show, false to hide
     * @return this builder
     */
    public TimerBuilder visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Set whether the timer name is shown.
     *
     * @param showName true to show name, false to hide
     * @return this builder
     */
    public TimerBuilder showName(boolean showName) {
        this.showName = showName;
        return this;
    }

    /**
     * Set the animation type.
     *
     * @param animationType the animation type
     * @return this builder
     */
    public TimerBuilder animationType(AnimationType animationType) {
        this.animationType = animationType;
        return this;
    }

    /**
     * Set the first color for animations (hex format).
     *
     * @param color1 the color in hex format (e.g., "#FF0000")
     * @return this builder
     */
    public TimerBuilder color1(String color1) {
        this.color1 = color1;
        return this;
    }

    /**
     * Set the second color for animations (hex format).
     *
     * @param color2 the color in hex format (e.g., "#0000FF")
     * @return this builder
     */
    public TimerBuilder color2(String color2) {
        this.color2 = color2;
        return this;
    }

    /**
     * Set the animation speed multiplier.
     *
     * @param speed the animation speed (0.1 to 10.0)
     * @return this builder
     */
    public TimerBuilder animationSpeed(double speed) {
        this.animationSpeed = speed;
        return this;
    }

    /**
     * Set the animation duration in ticks for time changes.
     *
     * @param ticks the duration in ticks (1 to 100)
     * @return this builder
     */
    public TimerBuilder animationDurationTicks(int ticks) {
        this.animationDurationTicks = ticks;
        return this;
    }

    /**
     * Set the maximum time limit for the timer in seconds.
     * When the timer reaches this time, it will stop automatically.
     * Set to 0 for no limit.
     *
     * @param seconds the maximum time in seconds
     * @return this builder
     */
    public TimerBuilder maxTime(long seconds) {
        this.maxTime = seconds;
        return this;
    }

    /**
     * Set whether to display the maximum time in the timer display.
     *
     * @param showMaxTime true to show max time, false otherwise
     * @return this builder
     */
    public TimerBuilder showMaxTime(boolean showMaxTime) {
        this.showMaxTime = showMaxTime;
        return this;
    }

    /**
     * Set a command to execute when the timer reaches the maximum time.
     *
     * @param command the command to execute (without leading slash)
     * @return this builder
     */
    public TimerBuilder maxTargetCommand(String command) {
        this.maxTargetCommand = command;
        return this;
    }

    /**
     * Build and create the timer.
     *
     * @return the created TimerInstance, or null if creation failed
     * @throws IllegalArgumentException if required fields are missing
     */
    public TimerInstance build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Timer name is required");
        }


        boolean created = api.getMultiTimerManager().createTimer(name, type, targetId);
        if (!created) {
            return null;
        }


        TimerInstance timer = api.getTimer(name);
        if (timer == null) {
            return null;
        }


        timer.setCountingUp(countingUp);
        timer.setCurrentTime(initialTime);
        timer.setVisible(visible);
        timer.setShowName(showName);

        timer.setAnimationType(animationType);
        timer.setColor1(color1);
        timer.setColor2(color2);
        timer.setAnimationSpeed(animationSpeed);
        timer.setAnimationDurationTicks(animationDurationTicks);

        timer.setMaxTime(maxTime);
        timer.setShowMaxTime(showMaxTime);
        timer.setMaxTargetCommand(maxTargetCommand);

        if (startRunning) {
            timer.start();
        }


        api.getMultiTimerManager().saveToConfig();

        return timer;
    }
}
