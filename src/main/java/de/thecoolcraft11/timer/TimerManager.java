package de.thecoolcraft11.timer;

import de.thecoolcraft11.timer.api.events.TimerResetEvent;
import de.thecoolcraft11.timer.api.events.TimerStartEvent;
import de.thecoolcraft11.timer.api.events.TimerStopEvent;
import de.thecoolcraft11.timer.api.events.TimerTimeChangeEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimerManager {
    private final Timer plugin;
    private long currentTime;
    private boolean running;
    private boolean countingUp;
    private final Map<String, TimerTarget> targets;
    private long animationFrame;
    private long tickCounter;
    private double animationSpeed;


    private boolean isAnimatingTime;
    private long animationStartTime;
    private long animationTargetTime;
    private long animationStartTick;
    private int animationDurationTicks;


    private boolean showActionbar;
    private long maxTime;
    private boolean showMaxTime;
    private String maxTargetCommand;


    private final Map<String, IntegrationTarget> integrationTargets;

    public TimerManager(Timer plugin) {
        this.plugin = plugin;
        this.running = false;
        this.countingUp = true;
        this.currentTime = 0;
        this.targets = new ConcurrentHashMap<>();
        this.integrationTargets = new ConcurrentHashMap<>();
        this.animationFrame = 0;
        this.tickCounter = 0;
        this.animationSpeed = 1.0;
        this.isAnimatingTime = false;
        this.animationStartTime = 0;
        this.animationTargetTime = 0;
        this.animationStartTick = 0;
        this.animationDurationTicks = 10;
        this.showActionbar = true;
        this.maxTime = 0;
        this.showMaxTime = false;
        this.maxTargetCommand = null;
        loadFromConfig();
    }

    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        this.currentTime = config.getLong("timer.current-time", 0);
        this.countingUp = config.getBoolean("timer.counting-up", true);
        this.animationDurationTicks = config.getInt("timer.animation.duration-ticks", 10);
        this.animationSpeed = config.getDouble("timer.animation.speed", 1.0);
        this.showActionbar = config.getBoolean("timer.show-actionbar", true);
        this.maxTime = config.getLong("timer.max-time", 0);
        this.showMaxTime = config.getBoolean("timer.show-max-time", false);
        this.maxTargetCommand = config.getString("timer.max-target-command", null);


        if (this.animationDurationTicks <= 0) {
            this.animationDurationTicks = 1;
        }


        if (this.animationSpeed < 0.1) {
            this.animationSpeed = 0.1;
        } else if (this.animationSpeed > 10.0) {
            this.animationSpeed = 10.0;
        }


        targets.clear();
        ConfigurationSection targetsSection = config.getConfigurationSection("timer.targets");
        if (targetsSection != null) {
            for (String key : targetsSection.getKeys(false)) {
                long time = targetsSection.getLong(key + ".time");
                String command = targetsSection.getString(key + ".command");
                targets.put(key, new TimerTarget(key, time, command));
            }
        }
    }

    public void saveToConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("timer.current-time", currentTime);
        config.set("timer.counting-up", countingUp);
        config.set("timer.show-actionbar", showActionbar);
        config.set("timer.max-time", maxTime);
        config.set("timer.show-max-time", showMaxTime);
        config.set("timer.max-target-command", maxTargetCommand);


        config.set("timer.targets", null);
        for (TimerTarget target : targets.values()) {
            config.set("timer.targets." + target.getId() + ".time", target.getTime());
            config.set("timer.targets." + target.getId() + ".command", target.getCommand());
        }
        plugin.saveConfig();
    }

    /**
     * Set whether the timer should be visible on players' action bars.
     */
    public void setActionbarVisible(boolean visible) {
        this.showActionbar = visible;
    }

    /**
     * Returns whether the timer is currently shown on players' action bars.
     */
    public boolean isActionbarVisible() {
        return showActionbar;
    }

    public void tick() {

        animationFrame++;
        tickCounter++;


        if (isAnimatingTime) {
            long ticksElapsed = animationFrame - animationStartTick;

            if (ticksElapsed >= animationDurationTicks) {

                currentTime = animationTargetTime;
                isAnimatingTime = false;
            } else {

                float progress = (float) ticksElapsed / animationDurationTicks;

                float easedProgress = 1 - (float) Math.pow(1 - progress, 3);
                currentTime = animationStartTime + (long) ((animationTargetTime - animationStartTime) * easedProgress);
            }
            return;
        }

        if (!running) return;


        if (tickCounter % 20 != 0) return;

        long previousTime = currentTime;

        if (countingUp) {
            currentTime++;


            de.thecoolcraft11.timer.api.events.TimerTickEvent tickEvent =
                    new de.thecoolcraft11.timer.api.events.TimerTickEvent(null, true, previousTime, currentTime);
            Bukkit.getPluginManager().callEvent(tickEvent);

            if (maxTime > 0 && currentTime >= maxTime) {
                currentTime = maxTime;
                TimerStopEvent stopEvent = new TimerStopEvent(null, true, TimerStopEvent.StopReason.MAX_TIME_REACHED);
                Bukkit.getPluginManager().callEvent(stopEvent);
                if (!stopEvent.isCancelled()) {
                    running = false;
                }

                if (maxTargetCommand != null && !maxTargetCommand.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), maxTargetCommand);
                        plugin.getLogger().info("Executed max time target command: " + maxTargetCommand);
                    });
                }
            }
            checkTargets();
        } else {
            checkTargets();
            currentTime--;


            de.thecoolcraft11.timer.api.events.TimerTickEvent tickEvent =
                    new de.thecoolcraft11.timer.api.events.TimerTickEvent(null, true, previousTime, currentTime);
            Bukkit.getPluginManager().callEvent(tickEvent);

            if (currentTime < 0) {
                currentTime = 0;
                TimerStopEvent stopEvent = new TimerStopEvent(null, true, TimerStopEvent.StopReason.COUNTDOWN_COMPLETE);
                Bukkit.getPluginManager().callEvent(stopEvent);
                if (!stopEvent.isCancelled()) {
                    running = false;
                }
            }
        }
    }

    private void checkTargets() {
        for (TimerTarget target : targets.values()) {
            if (target.isExecuted()) continue;


            boolean shouldExecute = (currentTime == target.getTime());

            if (shouldExecute) {
                executeTargetCommand(target);
                target.setExecuted(true);
            }
        }


        for (IntegrationTarget it : integrationTargets.values()) {
            if (it.isExecuted()) continue;
            boolean shouldExecute = (currentTime == it.getTime());
            if (shouldExecute) {
                executeIntegrationTarget(it);
                it.setExecuted();
            }
        }
    }

    private void executeTargetCommand(TimerTarget target) {
        de.thecoolcraft11.timer.api.events.TimerTargetExecuteEvent event =
                new de.thecoolcraft11.timer.api.events.TimerTargetExecuteEvent(null, true, target, currentTime);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        if (target.getCommand() != null && !target.getCommand().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), target.getCommand());
                plugin.getLogger().info("Executed target command '" + target.getId() + "': " + target.getCommand());
            });
        }
    }

    private void executeIntegrationTarget(IntegrationTarget it) {
        if (it.getAction() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    it.getAction().run();
                    plugin.getLogger().info("Executed integration target '" + it.getId() + "'");
                } catch (Exception e) {
                    plugin.getLogger().severe(
                            "Error executing integration target '" + it.getId() + "': " + e.getMessage());
                }
            });
        }
    }

    public Component getDisplayText() {
        String timeStr = formatTime(currentTime);

        if (showMaxTime && maxTime > 0) {
            String maxTimeStr = formatTime(maxTime);
            timeStr = timeStr + " / " + maxTimeStr;
        }


        return applyColorAnimation(timeStr);
    }

    private Component applyColorAnimation(String timeStr) {
        AnimationType animationType = AnimationType.fromString(
                plugin.getConfig().getString("timer.animation.type", "gradient"));
        String color1 = plugin.getConfig().getString("timer.animation.color1", "#00FF00");
        String color2 = plugin.getConfig().getString("timer.animation.color2", "#0080FF");

        return switch (animationType) {
            case WAVE -> createWaveAnimation(timeStr, color1, color2);
            case PULSE -> createPulseAnimation(timeStr, color1, color2);
            case RAINBOW -> createRainbowAnimation(timeStr);
            case STILL -> Component.text(timeStr)
                    .color(net.kyori.adventure.text.format.TextColor.fromHexString(color1))
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
            default -> createGradientAnimation(timeStr, color1, color2);
        };
    }

    private Component createGradientAnimation(String timeStr, String color1, String color2) {
        Component result = Component.empty();
        int length = timeStr.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / Math.max(1, length - 1);
            String interpolatedColor = interpolateColor(color1, color2, ratio);
            result = result.append(Component.text(String.valueOf(timeStr.charAt(i)))
                    .color(net.kyori.adventure.text.format.TextColor.fromHexString(interpolatedColor))
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        }

        return result;
    }

    private Component createWaveAnimation(String timeStr, String color1, String color2) {
        Component result = Component.empty();


        double phase = animationFrame * animationSpeed * 0.5;
        int length = timeStr.length();

        for (int i = 0; i < length; i++) {
            float wave = (float) Math.sin((i * 0.5 + phase) * 0.5) * 0.5f + 0.5f;
            String interpolatedColor = interpolateColor(color1, color2, wave);
            result = result.append(Component.text(String.valueOf(timeStr.charAt(i)))
                    .color(net.kyori.adventure.text.format.TextColor.fromHexString(interpolatedColor))
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        }

        return result;
    }

    private Component createPulseAnimation(String timeStr, String color1, String color2) {

        float pulse = (float) Math.sin(animationFrame * animationSpeed * 0.1) * 0.5f + 0.5f;
        String interpolatedColor = interpolateColor(color1, color2, pulse);
        return Component.text(timeStr)
                .color(net.kyori.adventure.text.format.TextColor.fromHexString(interpolatedColor))
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
    }

    private Component createRainbowAnimation(String timeStr) {
        Component result = Component.empty();
        int length = timeStr.length();


        double phase = (animationFrame * animationSpeed) % 360.0;

        for (int i = 0; i < length; i++) {

            double hue = (i * 360.0 / length + phase);
            int wrappedHue = (int) (hue % 360.0);
            if (wrappedHue < 0) wrappedHue += 360;

            String color = hslToHex(wrappedHue);
            result = result.append(Component.text(String.valueOf(timeStr.charAt(i)))
                    .color(net.kyori.adventure.text.format.TextColor.fromHexString(color))
                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        }

        return result;
    }

    private String interpolateColor(String color1, String color2, float ratio) {
        int r1 = Integer.parseInt(color1.substring(1, 3), 16);
        int g1 = Integer.parseInt(color1.substring(3, 5), 16);
        int b1 = Integer.parseInt(color1.substring(5, 7), 16);

        int r2 = Integer.parseInt(color2.substring(1, 3), 16);
        int g2 = Integer.parseInt(color2.substring(3, 5), 16);
        int b2 = Integer.parseInt(color2.substring(5, 7), 16);

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return String.format("#%02X%02X%02X", r, g, b);
    }

    private String hslToHex(int hue) {
        float h = hue / 360.0f;
        float s = 100 / 100.0f;
        float l = 50 / 100.0f;

        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;

        float r, g, b;

        if (h < 1.0f / 6) {
            r = c;
            g = x;
            b = 0;
        } else if (h < 2.0f / 6) {
            r = x;
            g = c;
            b = 0;
        } else if (h < 3.0f / 6) {
            r = 0;
            g = c;
            b = x;
        } else if (h < 4.0f / 6) {
            r = 0;
            g = x;
            b = c;
        } else if (h < 5.0f / 6) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }

        int red = (int) ((r + m) * 255);
        int green = (int) ((g + m) * 255);
        int blue = (int) ((b + m) * 255);

        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    public void start() {
        TimerStartEvent event = new TimerStartEvent(null, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        running = true;
        resetAllTargets();
    }

    public void stop() {
        TimerStopEvent event = new TimerStopEvent(null, true, TimerStopEvent.StopReason.MANUAL);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        running = false;
    }

    public void pause() {
        TimerStopEvent event = new TimerStopEvent(null, true, TimerStopEvent.StopReason.MANUAL);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        running = false;
    }

    public void resume() {
        TimerStartEvent event = new TimerStartEvent(null, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        running = true;
    }

    public void reset() {
        TimerResetEvent resetEvent = new TimerResetEvent(null, true, currentTime);
        Bukkit.getPluginManager().callEvent(resetEvent);
        if (resetEvent.isCancelled()) {
            return;
        }

        currentTime = 0;
        if (running) {
            TimerStopEvent stopEvent = new TimerStopEvent(null, true, TimerStopEvent.StopReason.RESET);
            Bukkit.getPluginManager().callEvent(stopEvent);
            running = false;
        }
        resetAllTargets();
    }

    public void setTime(long seconds) {
        TimerTimeChangeEvent event = new TimerTimeChangeEvent(null, true, currentTime, seconds);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        animateToTime(event.getNewTime());
    }

    public void setTimeInstant(long seconds) {
        currentTime = seconds;
        isAnimatingTime = false;
        resetAllTargets();
    }

    public void animateToTime(long targetSeconds) {
        isAnimatingTime = true;
        animationStartTime = currentTime;
        animationTargetTime = targetSeconds;
        animationStartTick = animationFrame;
        resetAllTargets();
    }

    public boolean isAnimating() {
        return isAnimatingTime;
    }

    public void setCountingUp(boolean countingUp) {
        this.countingUp = countingUp;
        resetAllTargets();
    }

    private void resetAllTargets() {
        for (TimerTarget target : targets.values()) {
            target.reset();
        }

        for (IntegrationTarget it : integrationTargets.values()) {
            it.reset();
        }
    }


    public void addTarget(String id, long time, String command) {
        targets.put(id, new TimerTarget(id, time, command));
    }

    /**
     * Add an integration-only target which will execute the provided Runnable when the timer
     * reaches the specified time. Integration targets are not persisted and do not appear
     * individually in the target list; instead a single synthetic entry is shown when any
     * integration target exists.
     */
    public void addTarget(String id, long time, Runnable action) {
        integrationTargets.put(id, new IntegrationTarget(id, time, action));
    }

    public boolean removeTarget(String id) {
        boolean removed = targets.remove(id) != null;
        if (!removed) {
            removed = integrationTargets.remove(id) != null;
        }
        return removed;
    }

    public void clearAllTargets() {
        targets.clear();
        integrationTargets.clear();
    }

    public TimerTarget getTarget(String id) {
        return targets.get(id);
    }

    public Collection<TimerTarget> getAllTargets() {


        List<TimerTarget> combined = new ArrayList<>(targets.values());
        if (!integrationTargets.isEmpty()) {
            combined.add(new TimerTarget("integration-execution", 0, "Integration Execution is active"));
        }
        return combined;
    }

    public boolean hasNoTargets() {
        return targets.isEmpty() && integrationTargets.isEmpty();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isCountingUp() {
        return countingUp;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public double getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(double speed) {

        if (speed < 0.1) {
            this.animationSpeed = 0.1;
        } else this.animationSpeed = Math.min(speed, 10.0);
    }

    public void setAnimationDurationTicks(int ticks) {
        if (ticks < 1) {
            this.animationDurationTicks = 1;
        } else this.animationDurationTicks = Math.min(ticks, 100);
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long seconds) {
        this.maxTime = seconds;
        resetAllTargets();
    }

    public boolean isMaxTimeVisible() {
        return showMaxTime;
    }

    public void setMaxTimeVisible(boolean visible) {
        this.showMaxTime = visible;
    }

    public String getMaxTargetCommand() {
        return maxTargetCommand;
    }

    public void setMaxTarget(String command) {
        this.maxTargetCommand = command;
    }

    public void removeMaxTarget() {
        this.maxTargetCommand = null;
    }


    private static class IntegrationTarget {
        private final String id;
        private final long time;
        private final Runnable action;
        private boolean executed;

        IntegrationTarget(String id, long time, Runnable action) {
            this.id = id;
            this.time = time;
            this.action = action;
            this.executed = false;
        }

        String getId() {
            return id;
        }

        long getTime() {
            return time;
        }

        Runnable getAction() {
            return action;
        }

        boolean isExecuted() {
            return executed;
        }

        void setExecuted() {
            this.executed = true;
        }

        void reset() {
            this.executed = false;
        }
    }
}
