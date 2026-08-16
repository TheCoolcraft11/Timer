package de.thecoolcraft11.timer;

import de.thecoolcraft11.timer.api.TimerAPI;
import de.thecoolcraft11.timer.api.events.WorldResetEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
    private static final String DATA_FILE = "data.yml";

    private TimerManager timerManager;
    private TimerTask timerTask;
    private MultiTimerManager multiTimerManager;
    private TimerAPI api;
    private FileConfiguration dataConfig;
    private List<String> worldsToDeleteOnReset;
    private boolean deleteOnBoot;
    private boolean disallowCustomSeed;
    private boolean hideSeed;
    private boolean resetOnPlayerDeath;
    private boolean resetEnabled;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        reloadResetConfig();

        Path resetFile = getDataFolder().toPath().resolve(PENDING_RESET_FILE);

        if (!isResetEnabled()) {
            if (Files.exists(resetFile)) {
                getLogger().warning(
                        "Pending world reset detected, but reset features are disabled. The pending reset will not be applied.");
            }
            return;
        }

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
        loadDataConfig();

        timerManager = new TimerManager(this);
        multiTimerManager = new MultiTimerManager(this, timerManager);
        api = new TimerAPI(this);

        TimerCommand timerCommand = new TimerCommand(this, timerManager, multiTimerManager);
        Objects.requireNonNull(getCommand("timer")).setExecutor(timerCommand);
        Objects.requireNonNull(getCommand("timer")).setTabCompleter(timerCommand);

        if (isResetEnabled()) {
            ResetCommand resetCommand = new ResetCommand(this);
            Objects.requireNonNull(getCommand("reset")).setExecutor(resetCommand);
            Objects.requireNonNull(getCommand("reset")).setTabCompleter(resetCommand);

            getServer().getPluginManager().registerEvents(new ResetListener(this), this);
        } else {
            unregisterResetCommand();
            getLogger().info("Reset features are disabled in the config; /reset command was not registered.");
        }

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
        if (!isResetEnabled()) {
            getLogger().warning("World reset requested, but reset features are disabled in the config.");
            return;
        }

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
        if (!isResetEnabled()) {
            getLogger().warning("World reset requested, but reset features are disabled in the config.");
            return;
        }
        long seed = ThreadLocalRandom.current().nextLong();
        Bukkit.getScheduler().runTaskLater(this, () -> prepareWorldReset(seed), delayTicks);
    }

    public boolean isPendingWorldReset() {
        return isResetEnabled() && Files.exists(getDataFolder().toPath().resolve(PENDING_RESET_FILE));
    }

    private void unregisterResetCommand() {
        Command resetCommand = getCommand("reset");
        if (resetCommand != null) {
            resetCommand.unregister(getServer().getCommandMap());

            getServer().getCommandMap().getKnownCommands().remove("reset");

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.updateCommands();
            }
        }
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

    public TimerAPI getAPI() {
        return api;
    }

    private void loadDataConfig() {
        Path dataFile = getDataFolder().toPath().resolve(DATA_FILE);
        dataConfig = YamlConfiguration.loadConfiguration(dataFile.toFile());
        dataConfig.options().setHeader(List.of(
                "Runtime data automatically managed by the Timer plugin. Do not edit manually.\n",
                "config.yml holds your settings; this file holds plugin-generated state ",
                "(timer values, targets, animation overrides, multi-timers)."));
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    public void saveDataConfig() {
        if (dataConfig == null) {
            return;
        }
        Path dataFile = getDataFolder().toPath().resolve(DATA_FILE);
        try {
            Files.createDirectories(dataFile.getParent());
            dataConfig.save(dataFile.toFile());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save " + DATA_FILE + "!", e);
        }
    }

    public void reloadDataConfig() {
        loadDataConfig();
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
        this.resetEnabled = getConfig().getBoolean("reset.enabled", true);

        if (this.resetEnabled && this.deleteOnBoot) {
            getLogger().warning("DELETE-ON-BOOT is enabled! Worlds will be deleted on server start.");
        }
    }

    public List<String> getWorldsToDeleteOnReset() {
        return worldsToDeleteOnReset;
    }

    public boolean isCustomSeedDisallowed() {
        return disallowCustomSeed;
    }

    public boolean isSeedHidden() {
        return hideSeed;
    }

    public boolean isResetOnPlayerDeath() {
        return resetOnPlayerDeath;
    }

    public boolean isResetEnabled() {
        return resetEnabled;
    }

    public void setResetEnabled(boolean enabled, String source, String reason) {
        this.resetEnabled = enabled;

        String action = enabled ? "enabled" : "disabled";
        String log = "World reset features " + action + " "
                + (source == null ? "via API" : "by " + source)
                + (reason == null || reason.isBlank() ? "" : ": " + reason)
                + ".";
        getLogger().info(log);

        if (!enabled) {
            unregisterResetCommand();
        } else {
            ResetCommand resetCommand = new ResetCommand(this);
            Objects.requireNonNull(getCommand("reset")).setExecutor(resetCommand);
            Objects.requireNonNull(getCommand("reset")).setTabCompleter(resetCommand);
        }
    }
}
