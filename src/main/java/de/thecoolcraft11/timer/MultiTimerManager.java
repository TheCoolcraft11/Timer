package de.thecoolcraft11.timer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class MultiTimerManager {
    private final Timer plugin;
    private final Map<String, TimerInstance> timers;
    private final TimerManager globalTimer; 

    public MultiTimerManager(Timer plugin, TimerManager globalTimer) {
        this.plugin = plugin;
        this.globalTimer = globalTimer;
        this.timers = new HashMap<>();
        loadFromConfig();
    }

    public void loadFromConfig() {
        FileConfiguration config = plugin.getConfig();
        timers.clear();

        ConfigurationSection timersSection = config.getConfigurationSection("multi-timers");
        if (timersSection != null) {
            for (String timerName : timersSection.getKeys(false)) {
                String type = timersSection.getString(timerName + ".type", "GLOBAL");
                String targetId = timersSection.getString(timerName + ".target-id", null);
                long currentTime = timersSection.getLong(timerName + ".current-time", 0);
                boolean running = timersSection.getBoolean(timerName + ".running", false);
                boolean countingUp = timersSection.getBoolean(timerName + ".counting-up", true);
                boolean visible = timersSection.getBoolean(timerName + ".visible", true);
                boolean showName = timersSection.getBoolean(timerName + ".show-name", true);

                TimerInstance instance = new TimerInstance(timerName, TimerType.valueOf(type), targetId);
                instance.setCurrentTime(currentTime);
                instance.setRunning(running);
                instance.setCountingUp(countingUp);
                instance.setVisible(visible);
                instance.setShowName(showName);

                
                String animType = timersSection.getString(timerName + ".animation.type", "gradient");
                String color1 = timersSection.getString(timerName + ".animation.color1", "#00FF00");
                String color2 = timersSection.getString(timerName + ".animation.color2", "#0080FF");
                double animSpeed = timersSection.getDouble(timerName + ".animation.speed", 1.0);
                int animDuration = timersSection.getInt(timerName + ".animation.duration-ticks", 10);
                instance.setAnimationType(animType);
                instance.setColor1(color1);
                instance.setColor2(color2);
                instance.setAnimationSpeed(animSpeed);
                instance.setAnimationDurationTicks(animDuration);

                
                long maxTime = timersSection.getLong(timerName + ".max-time", 0);
                boolean showMaxTime = timersSection.getBoolean(timerName + ".show-max-time", false);
                String maxTargetCmd = timersSection.getString(timerName + ".max-target-command", null);
                instance.setMaxTime(maxTime);
                instance.setShowMaxTime(showMaxTime);
                instance.setMaxTargetCommand(maxTargetCmd);

                
                ConfigurationSection targetsSection = timersSection.getConfigurationSection(timerName + ".targets");
                if (targetsSection != null) {
                    for (String targetId2 : targetsSection.getKeys(false)) {
                        long targetTime = targetsSection.getLong(targetId + ".time");
                        String targetCommand = targetsSection.getString(targetId + ".command");
                        instance.addTarget(targetId2, targetTime, targetCommand);
                    }
                }

                timers.put(timerName, instance);
            }
        }
    }

    public void saveToConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("multi-timers", null); 

        for (Map.Entry<String, TimerInstance> entry : timers.entrySet()) {
            String name = entry.getKey();
            TimerInstance instance = entry.getValue();

            config.set("multi-timers." + name + ".type", instance.getType().name());
            config.set("multi-timers." + name + ".target-id", instance.getTargetId());
            config.set("multi-timers." + name + ".current-time", instance.getCurrentTime());
            config.set("multi-timers." + name + ".running", instance.isRunning());
            config.set("multi-timers." + name + ".counting-up", instance.isCountingUp());
            config.set("multi-timers." + name + ".visible", instance.isVisible());
            config.set("multi-timers." + name + ".show-name", instance.isShowName());

            
            config.set("multi-timers." + name + ".animation.type", instance.getAnimationType());
            config.set("multi-timers." + name + ".animation.color1", instance.getColor1());
            config.set("multi-timers." + name + ".animation.color2", instance.getColor2());
            config.set("multi-timers." + name + ".animation.speed", instance.getAnimationSpeed());
            config.set("multi-timers." + name + ".animation.duration-ticks", instance.getAnimationDurationTicks());

            
            config.set("multi-timers." + name + ".max-time", instance.getMaxTime());
            config.set("multi-timers." + name + ".show-max-time", instance.isShowMaxTime());
            config.set("multi-timers." + name + ".max-target-command", instance.getMaxTargetCommand());

            
            config.set("multi-timers." + name + ".targets", null);
            for (Map.Entry<String, TimerTarget> targetEntry : instance.getAllTargets().entrySet()) {
                TimerTarget target = targetEntry.getValue();
                config.set("multi-timers." + name + ".targets." + target.getId() + ".time", target.getTime());
                config.set("multi-timers." + name + ".targets." + target.getId() + ".command", target.getCommand());
            }
        }

        plugin.saveConfig();
    }

    public boolean createTimer(String name, TimerType type, String targetId) {
        if (timers.containsKey(name)) {
            return false; 
        }

        TimerInstance instance = new TimerInstance(name, type, targetId);
        timers.put(name, instance);
        saveToConfig();
        return true;
    }

    public boolean deleteTimer(String name) {
        if (timers.remove(name) != null) {
            saveToConfig();
            return true;
        }
        return false;
    }

    public TimerInstance getTimer(String name) {
        return timers.get(name);
    }

    public Collection<TimerInstance> getAllTimers() {
        return timers.values();
    }

    public List<TimerInstance> getTimersForPlayer(Player player) {
        List<TimerInstance> result = new ArrayList<>();
        for (TimerInstance instance : timers.values()) {
            if (instance.getType() == TimerType.PLAYER && player.getUniqueId().toString().equals(instance.getTargetId())) {
                result.add(instance);
            }
        }
        return result;
    }

    public List<TimerInstance> getTimersForTeam(String teamName) {
        List<TimerInstance> result = new ArrayList<>();
        for (TimerInstance instance : timers.values()) {
            if (instance.getType() == TimerType.TEAM && teamName.equals(instance.getTargetId())) {
                result.add(instance);
            }
        }
        return result;
    }

    public List<TimerInstance> getGlobalTimers() {
        List<TimerInstance> result = new ArrayList<>();
        for (TimerInstance instance : timers.values()) {
            if (instance.getType() == TimerType.GLOBAL) {
                result.add(instance);
            }
        }
        return result;
    }

    public void tick() {
        for (TimerInstance instance : timers.values()) {
            instance.tick();
        }
    }

    public TimerManager getGlobalTimer() {
        return globalTimer;
    }

    public boolean hasTimer(String name) {
        return timers.containsKey(name);
    }

    public enum TimerType {
        GLOBAL,   
        PLAYER,   
        TEAM      
    }
}

