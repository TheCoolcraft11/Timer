package de.thecoolcraft11.timer;

import de.thecoolcraft11.timer.api.TimerAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class Timer extends JavaPlugin {
    private static final String PENDING_RESET_FLAG = "pending-reset.flag";
    private TimerManager timerManager;
    private TimerTask timerTask;
    private MultiTimerManager multiTimerManager;
    private TimerAPI api;
    private List<String> worldsToDeleteOnReset;
    private boolean deleteOnBoot;
    private boolean deleteOnShutdown;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        reloadResetConfig();

        boolean hasPendingReset = hasPendingResetFlag();
        if (deleteOnBoot || hasPendingReset) {
            getLogger().warning("Boot world deletion requested. Deleting configured worlds before full startup...");

            ResetCommand resetCommand = new ResetCommand(this);
            boolean success = resetCommand.deleteConfiguredWorlds(false);

            if (success) {
                clearPendingResetFlag();
                getLogger().info("Boot world deletion completed.");
            } else {
                getLogger().severe("Boot world deletion did not fully succeed. Some worlds may still exist.");
            }
        }
    }

    @Override
    public void onEnable() {

        saveDefaultConfig();

        timerManager = new TimerManager(this);
        multiTimerManager = new MultiTimerManager(this, timerManager);
        api = new TimerAPI(this);

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


        if (deleteOnShutdown) {
            getLogger().warning("DELETE-ON-SHUTDOWN is enabled! Deleting configured worlds...");
            ResetCommand resetCommand = new ResetCommand(this);
            boolean success = resetCommand.deleteConfiguredWorlds(true);

            if (success) {
                clearPendingResetFlag();
                getLogger().info("Shutdown world deletion completed.");
            } else {
                createPendingResetFlag();
                getLogger().warning("Shutdown deletion was incomplete. Marked a pending reset for next boot.");
            }
        }

        getLogger().info("Timer plugin has been disabled!");
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public MultiTimerManager getMultiTimerManager() {
        return multiTimerManager;
    }

    /**
     * Get the Timer API for external plugin integration.
     *
     * @return the TimerAPI instance
     */
    public TimerAPI getAPI() {
        return api;
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

    private boolean hasPendingResetFlag() {
        return new File(getDataFolder(), PENDING_RESET_FLAG).exists();
    }

    private void createPendingResetFlag() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("Could not create plugin data folder to write pending reset flag.");
            return;
        }

        File flagFile = new File(dataFolder, PENDING_RESET_FLAG);
        if (flagFile.exists()) {
            return;
        }

        try {
            if (!flagFile.createNewFile()) {
                getLogger().warning("Could not create pending reset flag file.");
            }
        } catch (IOException e) {
            getLogger().warning("Failed to create pending reset flag file: " + e.getMessage());
        }
    }

    private void clearPendingResetFlag() {
        File flagFile = new File(getDataFolder(), PENDING_RESET_FLAG);
        if (flagFile.exists() && !flagFile.delete()) {
            getLogger().warning("Could not remove pending reset flag file.");
        }
    }
}
