package de.thecoolcraft11.timer.api;

import de.thecoolcraft11.timer.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.List;

/**
 * Main API interface for external plugins to interact with the Timer plugin.
 * Access this API through the Timer plugin instance.
 *
 * <p>Example usage:
 * <pre>
 * Timer timerPlugin = (Timer) Bukkit.getPluginManager().getPlugin("Timer");
 * TimerAPI api = timerPlugin.getAPI();
 *
 * 
 * api.createTimer()
 *    .name("myTimer")
 *    .type(TimerType.GLOBAL)
 *    .countingUp(true)
 *    .build();
 * </pre>
 */
public class TimerAPI {
    private final Timer plugin;
    private final TimerManager globalTimerManager;
    private final MultiTimerManager multiTimerManager;

    public TimerAPI(Timer plugin) {
        this.plugin = plugin;
        this.globalTimerManager = plugin.getTimerManager();
        this.multiTimerManager = plugin.getMultiTimerManager();
    }

    /**
     * Get the main global timer manager.
     *
     * @return the global TimerManager instance
     */
    public TimerManager getGlobalTimerManager() {
        return globalTimerManager;
    }

    /**
     * Get the multi-timer manager for handling multiple timers.
     *
     * @return the MultiTimerManager instance
     */
    public MultiTimerManager getMultiTimerManager() {
        return multiTimerManager;
    }

    /**
     * Create a new timer using a builder pattern.
     *
     * @return a new TimerBuilder instance
     */
    public TimerBuilder createTimer() {
        return new TimerBuilder(this, plugin);
    }

    /**
     * Create a new target for a timer using a builder pattern.
     *
     * @param timerNameOrNull the name of the timer (null for global timer)
     * @return a new TargetBuilder instance
     */
    public TargetBuilder createTarget(String timerNameOrNull) {
        return new TargetBuilder(this, timerNameOrNull);
    }

    /**
     * Get a specific timer by name.
     *
     * @param name the timer name
     * @return the TimerInstance, or null if not found
     */
    public TimerInstance getTimer(String name) {
        return multiTimerManager.getTimer(name);
    }

    /**
     * Get all timers.
     *
     * @return collection of all TimerInstance objects
     */
    public Collection<TimerInstance> getAllTimers() {
        return multiTimerManager.getAllTimers();
    }

    /**
     * Get all timers for a specific player.
     *
     * @param player the player
     * @return list of TimerInstance objects for the player
     */
    public List<TimerInstance> getPlayerTimers(Player player) {
        return multiTimerManager.getTimersForPlayer(player);
    }

    /**
     * Get all timers for a specific team.
     *
     * @param teamName the team name
     * @return list of TimerInstance objects for the team
     */
    public List<TimerInstance> getTeamTimers(String teamName) {
        return multiTimerManager.getTimersForTeam(teamName);
    }

    /**
     * Get all global timers.
     *
     * @return list of global TimerInstance objects
     */
    public List<TimerInstance> getGlobalTimers() {
        return multiTimerManager.getGlobalTimers();
    }

    /**
     * Delete a timer by name.
     *
     * @param name the timer name
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteTimer(String name) {
        return multiTimerManager.deleteTimer(name);
    }

    /**
     * Check if a timer exists.
     *
     * @param name the timer name
     * @return true if the timer exists, false otherwise
     */
    public boolean hasTimer(String name) {
        return multiTimerManager.hasTimer(name);
    }

    /**
     * Start the global timer.
     */
    public void startGlobalTimer() {
        globalTimerManager.start();
    }

    /**
     * Stop the global timer.
     */
    public void stopGlobalTimer() {
        globalTimerManager.stop();
    }

    /**
     * Pause the global timer.
     */
    public void pauseGlobalTimer() {
        globalTimerManager.pause();
    }

    /**
     * Resume the global timer.
     */
    public void resumeGlobalTimer() {
        globalTimerManager.resume();
    }

    /**
     * Reset the global timer to zero.
     */
    public void resetGlobalTimer() {
        globalTimerManager.reset();
    }

    /**
     * Set the global timer time.
     *
     * @param seconds the time in seconds
     */
    public void setGlobalTimerTime(long seconds) {
        globalTimerManager.setTime(seconds);
    }

    /**
     * Get the current global timer time.
     *
     * @return the current time in seconds
     */
    public long getGlobalTimerTime() {
        return globalTimerManager.getCurrentTime();
    }

    /**
     * Check if the global timer is running.
     *
     * @return true if running, false otherwise
     */
    public boolean isGlobalTimerRunning() {
        return globalTimerManager.isRunning();
    }

    /**
     * Add a command target to the global timer.
     *
     * @param id      unique identifier for the target
     * @param time    the time in seconds when the target should execute
     * @param command the command to execute
     */
    public void addGlobalTimerTarget(String id, long time, String command) {
        globalTimerManager.addTarget(id, time, command);
    }

    /**
     * Add an integration target (Runnable) to the global timer.
     * This allows your plugin to execute custom code at a specific time.
     *
     * @param id     unique identifier for the target
     * @param time   the time in seconds when the target should execute
     * @param action the Runnable to execute
     */
    public void addGlobalTimerTarget(String id, long time, Runnable action) {
        globalTimerManager.addTarget(id, time, action);
    }

    /**
     * Remove a target from the global timer.
     *
     * @param id the target identifier
     * @return true if removed successfully, false otherwise
     */
    public boolean removeGlobalTimerTarget(String id) {
        return globalTimerManager.removeTarget(id);
    }

    /**
     * Get a specific target from the global timer.
     *
     * @param id the target identifier
     * @return the TimerTarget, or null if not found
     */
    public TimerTarget getGlobalTimerTarget(String id) {
        return globalTimerManager.getTarget(id);
    }

    /**
     * Get all targets from the global timer.
     *
     * @return collection of all TimerTarget objects
     */
    public Collection<TimerTarget> getGlobalTimerTargets() {
        return globalTimerManager.getAllTargets();
    }

    /**
     * Clear all targets from the global timer.
     */
    public void clearGlobalTimerTargets() {
        globalTimerManager.clearAllTargets();
    }

    /**
     * Add an integration target to a specific timer instance.
     *
     * @param timerName the name of the timer
     * @param id        unique identifier for the target
     * @param time      the time in seconds when the target should execute
     * @param action    the Runnable to execute
     * @return true if added successfully, false if timer not found
     */
    public boolean addTimerTarget(String timerName, String id, long time, Runnable action) {
        return multiTimerManager.addIntegrationTarget(timerName, id, time, action);
    }

    /**
     * Remove a target from a specific timer instance.
     *
     * @param timerName the name of the timer
     * @param id        the target identifier
     * @return true if removed successfully, false otherwise
     */
    public boolean removeTimerTarget(String timerName, String id) {
        return multiTimerManager.removeIntegrationTarget(timerName, id);
    }

    /**
     * Get the Timer plugin instance.
     *
     * @return the Timer plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }



    /**
     * Start a specific timer.
     *
     * @param timerName the name of the timer
     * @return true if started successfully, false if timer not found
     */
    public boolean startTimer(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.start();
        return true;
    }

    /**
     * Stop a specific timer.
     *
     * @param timerName the name of the timer
     * @return true if stopped successfully, false if timer not found
     */
    public boolean stopTimer(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.stop();
        return true;
    }

    /**
     * Pause a specific timer.
     *
     * @param timerName the name of the timer
     * @return true if paused successfully, false if timer not found
     */
    public boolean pauseTimer(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.pause();
        return true;
    }

    /**
     * Resume a specific timer.
     *
     * @param timerName the name of the timer
     * @return true if resumed successfully, false if timer not found
     */
    public boolean resumeTimer(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.resume();
        return true;
    }

    /**
     * Reset a specific timer to zero.
     *
     * @param timerName the name of the timer
     * @return true if reset successfully, false if timer not found
     */
    public boolean resetTimer(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.reset();
        return true;
    }

    /**
     * Set the time of a specific timer instantly.
     *
     * @param timerName the name of the timer
     * @param seconds   the time in seconds
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerTime(String timerName, long seconds) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setTimeInstant(seconds);
        return true;
    }

    /**
     * Animate the timer to a specific time with a smooth transition.
     *
     * @param timerName the name of the timer
     * @param seconds   the target time in seconds
     * @return true if animation started successfully, false if timer not found
     */
    public boolean animateTimerToTime(String timerName, long seconds) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.animateToTime(seconds);
        return true;
    }

    /**
     * Get the current time of a specific timer.
     *
     * @param timerName the name of the timer
     * @return the current time in seconds, or -1 if timer not found
     */
    public long getTimerTime(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return -1;
        return timer.getCurrentTime();
    }

    /**
     * Check if a specific timer is running.
     *
     * @param timerName the name of the timer
     * @return true if running, false if not running or timer not found
     */
    public boolean isTimerRunning(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        return timer.isRunning();
    }



    /**
     * Set whether a timer counts up or down.
     *
     * @param timerName  the name of the timer
     * @param countingUp true for counting up, false for counting down
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerCountingUp(String timerName, boolean countingUp) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setCountingUp(countingUp);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set whether a timer is visible to players.
     *
     * @param timerName the name of the timer
     * @param visible   true to show, false to hide
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerVisible(String timerName, boolean visible) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setVisible(visible);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set whether a timer's name is shown in the display.
     *
     * @param timerName the name of the timer
     * @param showName  true to show name, false to hide
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerShowName(String timerName, boolean showName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setShowName(showName);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set the animation type for a timer.
     *
     * @param timerName     the name of the timer
     * @param animationType the animation type
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerAnimationType(String timerName, AnimationType animationType) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setAnimationType(animationType);
        multiTimerManager.saveToConfig();
        return true;
    }


    /**
     * Set the first color for timer animations (hex format).
     *
     * @param timerName the name of the timer
     * @param color1    the color in hex format (e.g., "#FF0000")
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerColor1(String timerName, String color1) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setColor1(color1);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set the second color for timer animations (hex format).
     *
     * @param timerName the name of the timer
     * @param color2    the color in hex format (e.g., "#0000FF")
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerColor2(String timerName, String color2) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setColor2(color2);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set both colors for timer animations at once (hex format).
     *
     * @param timerName the name of the timer
     * @param color1    the first color in hex format (e.g., "#FF0000")
     * @param color2    the second color in hex format (e.g., "#0000FF")
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerColors(String timerName, String color1, String color2) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setColor1(color1);
        timer.setColor2(color2);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set the animation speed multiplier for a timer.
     *
     * @param timerName the name of the timer
     * @param speed     the animation speed (0.1 to 10.0)
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerAnimationSpeed(String timerName, double speed) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setAnimationSpeed(speed);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set the animation duration in ticks for time changes.
     *
     * @param timerName the name of the timer
     * @param ticks     the duration in ticks (1 to 100)
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerAnimationDurationTicks(String timerName, int ticks) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setAnimationDurationTicks(ticks);
        multiTimerManager.saveToConfig();
        return true;
    }



    /**
     * Set the maximum time limit for a timer in seconds.
     * When the timer reaches this time, it will stop automatically.
     * Set to 0 for no limit.
     *
     * @param timerName the name of the timer
     * @param seconds   the maximum time in seconds
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerMaxTime(String timerName, long seconds) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setMaxTime(seconds);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set whether to display the maximum time in the timer display.
     *
     * @param timerName   the name of the timer
     * @param showMaxTime true to show max time, false otherwise
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerShowMaxTime(String timerName, boolean showMaxTime) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setShowMaxTime(showMaxTime);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Set a command to execute when the timer reaches the maximum time.
     *
     * @param timerName the name of the timer
     * @param command   the command to execute (without leading slash)
     * @return true if set successfully, false if timer not found
     */
    public boolean setTimerMaxTargetCommand(String timerName, String command) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        timer.setMaxTargetCommand(command);
        multiTimerManager.saveToConfig();
        return true;
    }

    /**
     * Get the maximum time limit for a timer.
     *
     * @param timerName the name of the timer
     * @return the maximum time in seconds, or -1 if timer not found
     */
    public long getTimerMaxTime(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return -1;
        return timer.getMaxTime();
    }



    /**
     * Get whether a timer counts up or down.
     *
     * @param timerName the name of the timer
     * @return true if counting up, false if counting down or timer not found
     */
    public boolean isTimerCountingUp(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        return timer.isCountingUp();
    }

    /**
     * Get whether a timer is visible.
     *
     * @param timerName the name of the timer
     * @return true if visible, false if hidden or timer not found
     */
    public boolean isTimerVisible(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        return timer.isVisible();
    }

    /**
     * Get whether a timer's name is shown.
     *
     * @param timerName the name of the timer
     * @return true if name is shown, false otherwise or timer not found
     */
    public boolean isTimerShowingName(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return false;
        return timer.isShowName();
    }

    /**
     * Get the animation type of a timer.
     *
     * @param timerName the name of the timer
     * @return the animation type, or null if timer not found
     */
    public String getTimerAnimationTypeString(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return null;
        return timer.getAnimationType().toString();
    }

    /**
     * Get the animation type of a timer.
     *
     * @param timerName the name of the timer
     * @return the animation type, or null if timer not found
     */
    public AnimationType getTimerAnimationType(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return null;
        return timer.getAnimationType();
    }

    /**
     * Get the first color of a timer.
     *
     * @param timerName the name of the timer
     * @return the color in hex format, or null if timer not found
     */
    public String getTimerColor1(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return null;
        return timer.getColor1();
    }

    /**
     * Get the second color of a timer.
     *
     * @param timerName the name of the timer
     * @return the color in hex format, or null if timer not found
     */
    public String getTimerColor2(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return null;
        return timer.getColor2();
    }

    /**
     * Get the animation speed of a timer.
     *
     * @param timerName the name of the timer
     * @return the animation speed, or -1 if timer not found
     */
    public double getTimerAnimationSpeed(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return -1;
        return timer.getAnimationSpeed();
    }

    /**
     * Get the animation duration in ticks for a timer.
     *
     * @param timerName the name of the timer
     * @return the duration in ticks, or -1 if timer not found
     */
    public int getTimerAnimationDurationTicks(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return -1;
        return timer.getAnimationDurationTicks();
    }

    /**
     * Get the timer type.
     *
     * @param timerName the name of the timer
     * @return the TimerType, or null if timer not found
     */
    public MultiTimerManager.TimerType getTimerType(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return null;
        return timer.getType();
    }

    /**
     * Get the target ID for a PLAYER or TEAM timer.
     *
     * @param timerName the name of the timer
     * @return the target ID, or null if timer not found or is a GLOBAL timer
     */
    public String getTimerTargetId(String timerName) {
        TimerInstance timer = getTimer(timerName);
        if (timer == null) return null;
        return timer.getTargetId();
    }
}
