package de.thecoolcraft11.timer;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public final class Timer extends JavaPlugin {
    private TimerManager timerManager;
    private TimerTask timerTask;
    private MultiTimerManager multiTimerManager;
    private List<String> worldsToDeleteOnReset;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        timerManager = new TimerManager(this);
        multiTimerManager = new MultiTimerManager(this, timerManager);

        TimerCommand timerCommand = new TimerCommand(this, timerManager, multiTimerManager);
        Objects.requireNonNull(getCommand("timer")).setExecutor(timerCommand);
        Objects.requireNonNull(getCommand("timer")).setTabCompleter(timerCommand);

        ResetCommand resetCommand = new ResetCommand(this);
        Objects.requireNonNull(getCommand("reset")).setExecutor(resetCommand);
        Objects.requireNonNull(getCommand("reset")).setTabCompleter(resetCommand);

        reloadResetConfig();

        timerTask = new TimerTask(timerManager, multiTimerManager);
        timerTask.runTaskTimer(this, 0L, 1L);

        getLogger().info("Timer plugin has been enabled!");
    }

    @Override
    public void onDisable() {

        if (timerManager != null) {
            timerManager.saveToConfig();
        }

        if (multiTimerManager != null) {
            multiTimerManager.saveToConfig();
        }


        if (timerTask != null) {
            timerTask.cancel();
        }

        getLogger().info("Timer plugin has been disabled!");
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public MultiTimerManager getMultiTimerManager() {
        return multiTimerManager;
    }

    public void reloadResetConfig() {
        this.worldsToDeleteOnReset = getConfig().getStringList("reset.worlds-to-delete");
        if (this.worldsToDeleteOnReset.isEmpty()) {
            this.worldsToDeleteOnReset = List.of("world", "world_nether", "world_the_end");
        }
    }

    public List<String> getWorldsToDeleteOnReset() {
        return worldsToDeleteOnReset;
    }
}
