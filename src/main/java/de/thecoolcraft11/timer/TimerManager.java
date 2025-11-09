package de.thecoolcraft11.timer;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

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

    public TimerManager(Timer plugin) {
        this.plugin = plugin;
        this.running = false;
        this.countingUp = true;
        this.currentTime = 0;
        this.targets = new HashMap<>();
        this.animationFrame = 0;
        this.tickCounter = 0;
        this.animationSpeed = 1.0;
        this.isAnimatingTime = false;
        this.animationStartTime = 0;
        this.animationTargetTime = 0;
        this.animationStartTick = 0;
        this.animationDurationTicks = 10; 
        this.showActionbar = true; 
        loadFromConfig();
    }

    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        this.currentTime = config.getLong("timer.current-time", 0);
        this.countingUp = config.getBoolean("timer.counting-up", true);
        this.animationDurationTicks = config.getInt("timer.animation.set-animation-duration", 10);
        this.animationSpeed = config.getDouble("timer.animation.speed", 1.0);
        this.showActionbar = config.getBoolean("timer.show-actionbar", true);

        
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
                currentTime = animationStartTime + (long)((animationTargetTime - animationStartTime) * easedProgress);
            }
            return; 
        }

        if (!running) return;

        
        if (tickCounter % 20 != 0) return;

        if (countingUp) {
            currentTime++;
            checkTargets();
        } else {
            
            checkTargets();
            currentTime--;
            if (currentTime < 0) {
                currentTime = 0;
                running = false;
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
    }

    private void executeTargetCommand(TimerTarget target) {
        if (target.getCommand() != null && !target.getCommand().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), target.getCommand());
                plugin.getLogger().info("Executed target command '" + target.getId() + "': " + target.getCommand());
            });
        }
    }

    public Component getDisplayText() {
        String timeStr = formatTime(currentTime);

        
        return applyColorAnimation(timeStr);
    }

    private Component applyColorAnimation(String timeStr) {
        String animationType = plugin.getConfig().getString("timer.animation.type", "gradient");
        String color1 = plugin.getConfig().getString("timer.animation.color1", "#00FF00");
        String color2 = plugin.getConfig().getString("timer.animation.color2", "#0080FF");

        return switch (animationType.toLowerCase()) {
            case "wave" -> createWaveAnimation(timeStr, color1, color2);
            case "pulse" -> createPulseAnimation(timeStr, color1, color2);
            case "rainbow" -> createRainbowAnimation(timeStr);
            case "still" -> Component.text(timeStr)
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
            r = c; g = x; b = 0;
        } else if (h < 2.0f / 6) {
            r = x; g = c; b = 0;
        } else if (h < 3.0f / 6) {
            r = 0; g = c; b = x;
        } else if (h < 4.0f / 6) {
            r = 0; g = x; b = c;
        } else if (h < 5.0f / 6) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
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
        running = true;
        resetAllTargets();
    }

    public void stop() {
        running = false;
    }

    public void pause() {
        running = false;
    }

    public void resume() {
        running = true;
    }

    public void reset() {
        currentTime = 0;
        running = false;
        resetAllTargets();
    }

    public void setTime(long seconds) {
        animateToTime(seconds);
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
    }

    
    public void addTarget(String id, long time, String command) {
        targets.put(id, new TimerTarget(id, time, command));
    }

    public boolean removeTarget(String id) {
        return targets.remove(id) != null;
    }

    public void clearAllTargets() {
        targets.clear();
    }

    public TimerTarget getTarget(String id) {
        return targets.get(id);
    }

    public Collection<TimerTarget> getAllTargets() {
        return targets.values();
    }

    public boolean hasNoTargets() {
        return targets.isEmpty();
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
}

