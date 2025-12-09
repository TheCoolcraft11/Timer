package de.thecoolcraft11.timer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashMap;
import java.util.Map;

public class TimerInstance {
    private final String name;
    private final MultiTimerManager.TimerType type;
    private final String targetId;
    private long currentTime;
    private boolean running;
    private boolean countingUp;
    private long lastTickTime;
    private boolean visible;
    private boolean showName;


    private String animationType;
    private String color1;
    private String color2;
    private double animationSpeed;
    private long animationFrame;
    private int animationDurationTicks;


    private boolean isAnimatingTime;
    private long animationStartTime;
    private long animationTargetTime;
    private long animationStartTick;


    private long maxTime;
    private boolean showMaxTime;
    private String maxTargetCommand;


    private final Map<String, TimerTarget> targets;

    public TimerInstance(String name, MultiTimerManager.TimerType type, String targetId) {
        this.name = name;
        this.type = type;
        this.targetId = targetId;
        this.currentTime = 0;
        this.running = false;
        this.countingUp = true;
        this.lastTickTime = System.currentTimeMillis();
        this.visible = true;
        this.showName = true;


        this.animationType = "gradient";
        this.color1 = "#00FF00";
        this.color2 = "#0080FF";
        this.animationSpeed = 1.0;
        this.animationFrame = 0;
        this.animationDurationTicks = 10;


        this.isAnimatingTime = false;
        this.animationStartTime = 0;
        this.animationTargetTime = 0;
        this.animationStartTick = 0;


        this.maxTime = 0;
        this.showMaxTime = false;
        this.maxTargetCommand = null;


        this.targets = new HashMap<>();
    }

    public void tick() {

        animationFrame++;


        if (isAnimatingTime) {
            long ticksElapsed = animationFrame - animationStartTick;
            long ticksDuration = animationDurationTicks;

            if (ticksElapsed >= ticksDuration) {

                currentTime = animationTargetTime;
                isAnimatingTime = false;
            } else {

                double progress = (double) ticksElapsed / ticksDuration;
                long timeDiff = animationTargetTime - animationStartTime;
                currentTime = animationStartTime + (long) (timeDiff * progress);
            }
        }

        if (!running) return;

        long currentMillis = System.currentTimeMillis();
        if (currentMillis - lastTickTime >= 1000) {
            lastTickTime = currentMillis;

            if (countingUp) {
                currentTime++;


                if (maxTime > 0 && currentTime >= maxTime) {
                    currentTime = maxTime;
                    running = false;

                }

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
    }

    private void checkTargets() {
        for (TimerTarget target : targets.values()) {
            if (target.isExecuted()) continue;

            if (currentTime == target.getTime()) {
                target.setExecuted(true);

            }
        }
    }

    public String formatTime() {
        long hours = currentTime / 3600;
        long minutes = (currentTime % 3600) / 60;
        long seconds = currentTime % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public Component getDisplayComponent() {
        String timeStr = formatTime();


        if (showMaxTime && maxTime > 0) {
            String maxTimeStr = formatTime(maxTime);
            timeStr = timeStr + " / " + maxTimeStr;
        }

        Component timeComponent = applyColorAnimation(timeStr);

        if (showName) {
            String prefix = "[" + name + "] ";
            return Component.text(prefix)
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                    .append(timeComponent);
        } else {
            return timeComponent;
        }
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

    private Component applyColorAnimation(String timeStr) {
        return switch (animationType.toLowerCase()) {
            case "wave" -> createWaveAnimation(timeStr, color1, color2);
            case "pulse" -> createPulseAnimation(timeStr, color1, color2);
            case "rainbow" -> createRainbowAnimation(timeStr);
            case "still" -> Component.text(timeStr)
                    .color(net.kyori.adventure.text.format.TextColor.fromHexString(color1))
                    .decorate(TextDecoration.BOLD);
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
                    .decorate(TextDecoration.BOLD));
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
                    .decorate(TextDecoration.BOLD));
        }

        return result;
    }

    private Component createPulseAnimation(String timeStr, String color1, String color2) {
        float pulse = (float) Math.sin(animationFrame * animationSpeed * 0.1) * 0.5f + 0.5f;
        String interpolatedColor = interpolateColor(color1, color2, pulse);
        return Component.text(timeStr)
                .color(net.kyori.adventure.text.format.TextColor.fromHexString(interpolatedColor))
                .decorate(TextDecoration.BOLD);
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
                    .decorate(TextDecoration.BOLD));
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


    public String getName() {
        return name;
    }

    public MultiTimerManager.TimerType getType() {
        return type;
    }

    public String getTargetId() {
        return targetId;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
        if (running) {
            this.lastTickTime = System.currentTimeMillis();
        }
    }

    public boolean isCountingUp() {
        return countingUp;
    }

    public void setCountingUp(boolean countingUp) {
        this.countingUp = countingUp;
    }

    public void start() {
        this.running = true;
        this.lastTickTime = System.currentTimeMillis();
    }

    public void stop() {
        this.running = false;
    }

    public void pause() {
        this.running = false;
    }

    public void resume() {
        this.running = true;
        this.lastTickTime = System.currentTimeMillis();
    }

    public void reset() {
        this.currentTime = 0;
        this.running = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isShowName() {
        return showName;
    }

    public void setShowName(boolean showName) {
        this.showName = showName;
    }


    public String getAnimationType() {
        return animationType;
    }

    public void setAnimationType(String animationType) {
        this.animationType = animationType;
    }

    public String getColor1() {
        return color1;
    }

    public void setColor1(String color1) {
        this.color1 = color1;
    }

    public String getColor2() {
        return color2;
    }

    public void setColor2(String color2) {
        this.color2 = color2;
    }

    public double getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(double animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public int getAnimationDurationTicks() {
        return animationDurationTicks;
    }

    public void setAnimationDurationTicks(int durationTicks) {
        this.animationDurationTicks = durationTicks;
    }


    public void animateToTime(long targetSeconds) {
        isAnimatingTime = true;
        animationStartTime = currentTime;
        animationTargetTime = targetSeconds;
        animationStartTick = animationFrame;
        resetAllTargets();
    }

    public void setTimeInstant(long seconds) {
        currentTime = seconds;
        isAnimatingTime = false;
        resetAllTargets();
    }

    public boolean isAnimatingTime() {
        return isAnimatingTime;
    }

    public long getAnimationTargetTime() {
        return animationTargetTime;
    }


    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    public boolean isShowMaxTime() {
        return showMaxTime;
    }

    public void setShowMaxTime(boolean showMaxTime) {
        this.showMaxTime = showMaxTime;
    }

    public String getMaxTargetCommand() {
        return maxTargetCommand;
    }

    public void setMaxTargetCommand(String maxTargetCommand) {
        this.maxTargetCommand = maxTargetCommand;
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

    public Map<String, TimerTarget> getAllTargets() {
        return targets;
    }

    public void resetAllTargets() {
        for (TimerTarget target : targets.values()) {
            target.reset();
        }
    }
}

