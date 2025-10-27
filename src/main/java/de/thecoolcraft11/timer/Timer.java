package de.thecoolcraft11.timer;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Timer extends JavaPlugin {
    private TimerManager timerManager;
    private TimerTask timerTask;

    @Override
    public void onEnable() {
        
        saveDefaultConfig();

        timerManager = new TimerManager(this);
        
        TimerCommand timerCommand = new TimerCommand(this, timerManager);
        Objects.requireNonNull(getCommand("timer")).setExecutor(timerCommand);
        Objects.requireNonNull(getCommand("timer")).setTabCompleter(timerCommand);

        
        timerTask = new TimerTask(timerManager);
        timerTask.runTaskTimer(this, 0L, 1L); 

        getLogger().info("Timer plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        
        if (timerManager != null) {
            timerManager.saveToConfig();
        }

        
        if (timerTask != null) {
            timerTask.cancel();
        }

        getLogger().info("Timer plugin has been disabled!");
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }
}
