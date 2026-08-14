package de.thecoolcraft11.timer;

import de.thecoolcraft11.timer.api.TimerAPI;
import de.thecoolcraft11.timer.api.events.WorldResetEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class Timer extends JavaPlugin {
    private static final String PENDING_RESET_FILE = "pending-reset.properties";
    private static final String PENDING_RESET_SEED_KEY = "seed";

    private TimerManager timerManager;
    private TimerTask timerTask;
    private MultiTimerManager multiTimerManager;
    private TimerAPI api;
    private List<String> worldsToDeleteOnReset;
    private boolean deleteOnBoot;
    private boolean disallowCustomSeed;
    private boolean hideSeed;
    private boolean resetOnPlayerDeath;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        reloadResetConfig();

        Path resetFile = getDataFolder().toPath().resolve(PENDING_RESET_FILE);

        if (Files.exists(resetFile)) {
            getLogger().warning("Pending world reset detected!");

            try {
                Properties properties = new Properties();
                try (InputStream input = Files.newInputStream(resetFile)) {
                    properties.load(input);
                }

                String seed = properties.getProperty(PENDING_RESET_SEED_KEY);
                if (seed != null) {
                    getLogger().warning("Applying reset seed: " + seed);
                    setServerSeed(Long.parseLong(seed));
                }

                deleteConfiguredWorlds();

                Files.deleteIfExists(resetFile);

                getLogger().info("Pending world reset completed.");
            } catch (IOException | NumberFormatException e) {
                getLogger().log(Level.SEVERE, "Failed to perform pending world reset!", e);
            }
        } else if (deleteOnBoot) {
            getLogger().warning("DELETE-ON-BOOT is enabled! Deleting configured worlds before full startup...");
            deleteConfiguredWorlds();
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

        getServer().getPluginManager().registerEvents(new ResetListener(this), this);

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

    public void prepareWorldReset(long seed) {

        WorldResetEvent event = new WorldResetEvent(seed);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            getLogger().info("World reset was cancelled by an event listener.");
            return;
        }

        String seedText = hideSeed ? "" : " with seed " + seed;
        Bukkit.broadcast(
                Component.text("WARNING: The world is being reset" + seedText + "!").color(NamedTextColor.RED));
        Bukkit.broadcast(
                Component.text("All players will be kicked and the server will restart shortly...").color(
                        NamedTextColor.YELLOW));

        getLogger().info("Preparing complete world reset with seed: " + seed);

        Bukkit.getOnlinePlayers().forEach(player -> player.kick(
                Component.text("World reset in progress, please reconnect in a moment.").color(NamedTextColor.RED)));

        try {
            savePendingReset(seed);
            setServerSeed(seed);

            getLogger().info("World reset prepared successfully. Seed: " + seed);

        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not prepare world reset!", e);
            return;
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("Restarting server for world reset...");
            Bukkit.restart();
        }, 40L);
    }

    public void scheduleWorldReset(int delayTicks) {
        long seed = ThreadLocalRandom.current().nextLong();
        Bukkit.getScheduler().runTaskLater(this, () -> prepareWorldReset(seed), delayTicks);
    }

    public boolean isPendingWorldReset() {
        return Files.exists(getDataFolder().toPath().resolve(PENDING_RESET_FILE));
    }

    private void savePendingReset(long seed) throws IOException {
        Path resetFile = getDataFolder().toPath().resolve(PENDING_RESET_FILE);

        Files.createDirectories(getDataFolder().toPath());

        Properties properties = new Properties();
        properties.setProperty(PENDING_RESET_SEED_KEY, Long.toString(seed));

        try (OutputStream output = Files.newOutputStream(
                resetFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            properties.store(output, "Pending world reset - DO NOT EDIT");
        }

        getLogger().info("Saved pending reset information.");
    }

    private void setServerSeed(long seed) throws IOException {
        Path serverPropertiesPath = Bukkit.getWorldContainer().toPath().resolve("server.properties");

        List<String> lines = new ArrayList<>(Files.readAllLines(serverPropertiesPath));
        String seedEntry = "level-seed=" + seed;
        boolean replaced = false;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith("level-seed=")) {
                lines.set(i, seedEntry);
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            lines.add(seedEntry);
        }

        Files.write(serverPropertiesPath, lines);

        getLogger().info("Updated server.properties level-seed to " + seed);
    }

    private void deleteConfiguredWorlds() {
        List<String> patterns = worldsToDeleteOnReset;
        if (patterns == null || patterns.isEmpty()) {
            patterns = List.of("world", "world_nether", "world_the_end");
        }

        Path worldContainer = Bukkit.getWorldContainer().toPath();
        Set<String> targets = TimerUtil.resolveWorldFolders(worldContainer, patterns);

        if (targets.isEmpty()) {
            getLogger().info("No configured worlds found to delete.");
            return;
        }

        for (String worldName : targets) {
            getLogger().info("Deleting world folder: " + worldName);
            if (TimerUtil.deleteWorldFolder(worldContainer, worldName)) {
                getLogger().info("Deleted world folder: " + worldName);
            } else {
                getLogger().warning("Failed to delete world folder: " + worldName);
            }
        }
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
        this.disallowCustomSeed = getConfig().getBoolean("reset.disallow-custom-seed", true);
        this.hideSeed = getConfig().getBoolean("reset.hide-seed", true);
        this.resetOnPlayerDeath = getConfig().getBoolean("reset.reset-on-player-death", false);

        if (this.deleteOnBoot) {
            getLogger().warning("DELETE-ON-BOOT is enabled! Worlds will be deleted on server start.");
        }
    }

    public List<String> getWorldsToDeleteOnReset() {
        return worldsToDeleteOnReset;
    }

    /**
     * Whether players/admins are allowed to specify a custom seed via /reset.
     *
     * @return true if custom seeds are disallowed
     */
    public boolean isCustomSeedDisallowed() {
        return disallowCustomSeed;
    }

    /**
     * Whether the seed being used for a reset is hidden from players.
     *
     * @return true if the seed is hidden
     */
    public boolean isSeedHidden() {
        return hideSeed;
    }

    /**
     * Whether the world should reset instantly when a player dies.
     *
     * @return true if a player death triggers a world reset
     */
    public boolean isResetOnPlayerDeath() {
        return resetOnPlayerDeath;
    }
}
