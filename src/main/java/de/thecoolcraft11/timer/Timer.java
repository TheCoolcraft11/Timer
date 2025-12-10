package de.thecoolcraft11.timer;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public final class Timer extends JavaPlugin {
    private TimerManager timerManager;
    private TimerTask timerTask;
    private MultiTimerManager multiTimerManager;
    private List<String> worldsToDeleteOnReset;
    private boolean deleteOnBoot;
    private boolean deleteOnShutdown;

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

        
        if (deleteOnBoot) {
            getLogger().warning("DELETE-ON-BOOT is enabled! Deleting configured worlds...");
            resetCommand.deleteConfiguredWorlds();
            getLogger().info("Boot world deletion completed.");
        }

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

        
        if (deleteOnShutdown) {
            getLogger().warning("DELETE-ON-SHUTDOWN is enabled! Deleting configured worlds...");
            ResetCommand resetCommand = new ResetCommand(this);
            resetCommand.deleteConfiguredWorlds();
            getLogger().info("Shutdown world deletion completed.");
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
        this.deleteOnBoot = getConfig().getBoolean("reset.delete-on-boot", false);
        this.deleteOnShutdown = getConfig().getBoolean("reset.delete-on-shutdown", false);

        if (this.deleteOnBoot) {
            getLogger().warning("DELETE-ON-BOOT is enabled! Worlds will be deleted on server start.");
        }
        if (this.deleteOnShutdown) {
            getLogger().warning("DELETE-ON-SHUTDOWN is enabled! Worlds will be deleted on server shutdown.");
        }
    }

    public List<String> getWorldsToDeleteOnReset() {
        return worldsToDeleteOnReset;
    }
}
