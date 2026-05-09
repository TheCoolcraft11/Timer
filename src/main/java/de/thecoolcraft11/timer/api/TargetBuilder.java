package de.thecoolcraft11.timer.api;

import de.thecoolcraft11.timer.TimerInstance;

/**
 * Builder class for creating timer targets with a fluent API.
 *
 * <p>Example usage for command targets:
 * <pre>
 * api.createTarget("myTimer")
 *    .id("target1")
 *    .time(60)
 *    .command("say One minute reached!")
 *    .build();
 * </pre>
 *
 * <p>Example usage for integration targets:
 * <pre>
 * api.createTarget("myTimer")
 *    .id("custom-action")
 *    .time(120)
 *    .action(() -> {
 *        
 *        Bukkit.broadcastMessage("Two minutes!");
 *    })
 *    .build();
 * </pre>
 */
public class TargetBuilder {
    private final TimerAPI api;
    private final String timerName;

    private String id;
    private long time;
    private String command;
    private Runnable action;

    public TargetBuilder(TimerAPI api, String timerName) {
        this.api = api;
        this.timerName = timerName;
    }

    /**
     * Set the unique identifier for this target (required).
     *
     * @param id the target ID
     * @return this builder
     */
    public TargetBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Set the time in seconds when this target should execute (required).
     *
     * @param seconds the target time
     * @return this builder
     */
    public TargetBuilder time(long seconds) {
        this.time = seconds;
        return this;
    }

    /**
     * Set the command to execute when the target time is reached.
     * The command should not include the leading slash.
     *
     * @param command the command to execute
     * @return this builder
     */
    public TargetBuilder command(String command) {
        this.command = command;
        return this;
    }

    /**
     * Set a custom action (Runnable) to execute when the target time is reached.
     * This is useful for integration with other plugins.
     *
     * @param action the Runnable to execute
     * @return this builder
     */
    public TargetBuilder action(Runnable action) {
        this.action = action;
        return this;
    }

    /**
     * Build and add the target to the timer.
     *
     * @return true if the target was added successfully
     * @throws IllegalArgumentException if required fields are missing
     */
    public boolean build() {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Target ID is required");
        }

        if (command == null && action == null) {
            throw new IllegalArgumentException("Either command or action must be specified");
        }

        if (command != null && action != null) {
            throw new IllegalArgumentException("Cannot specify both command and action");
        }


        if (timerName == null) {
            if (action != null) {
                api.addGlobalTimerTarget(id, time, action);
            } else {
                api.addGlobalTimerTarget(id, time, command);
            }
            api.getGlobalTimerManager().saveToConfig();
            return true;
        } else {
            TimerInstance timer = api.getTimer(timerName);
            if (timer == null) {
                return false;
            }

            if (action != null) {
                timer.addTarget(id, time, action);
            } else {
                timer.addTarget(id, time, command);
            }
            api.getMultiTimerManager().saveToConfig();
            return true;
        }
    }
}
