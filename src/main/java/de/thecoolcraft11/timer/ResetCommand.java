package de.thecoolcraft11.timer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class ResetCommand implements CommandExecutor, TabCompleter {
    private final Timer plugin;

    public ResetCommand(Timer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("timer.admin")) {
            sender.sendMessage(
                    Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return true;
        }


        Long seed = null;
        if (args.length > 0) {
            try {
                seed = Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid seed! Must be a number.").color(NamedTextColor.RED));
                return true;
            }
        }


        String seedMessage = seed != null ? " with seed " + seed : " with a random seed";
        Bukkit.broadcast(
                Component.text("WARNING: The world is being reset" + seedMessage + "!").color(NamedTextColor.RED));
        Bukkit.broadcast(Component.text("All players will be kicked and the server will restart shortly...").color(
                NamedTextColor.YELLOW));

        final Long finalSeed = seed;


        Bukkit.getScheduler().runTaskLater(plugin, () -> performWorldReset(finalSeed), 60L);

        return true;
    }

    private void performWorldReset(Long seed) {

        if (seed == null) {
            seed = ThreadLocalRandom.current().nextLong();
        }
        final long resolvedSeed = seed;


        Bukkit.getOnlinePlayers().forEach(player -> player.kick(
                Component.text("World reset in progress, please reconnect in a moment.").color(NamedTextColor.RED)));

        plugin.getLogger().info("Resetting world with seed: " + resolvedSeed);

        boolean deleted = deleteConfiguredWorlds(true);
        if (!deleted) {
            plugin.getLogger().severe(
                    "World reset aborted: at least one world could not be unloaded/deleted. Check previous logs for details.");
            return;
        }


        WorldCreator overworldCreator = new WorldCreator("world");
        overworldCreator.environment(World.Environment.NORMAL);
        overworldCreator.seed(resolvedSeed);

        WorldCreator netherCreator = new WorldCreator("world_nether");
        netherCreator.environment(World.Environment.NETHER);
        netherCreator.seed(resolvedSeed);

        WorldCreator endCreator = new WorldCreator("world_the_end");
        endCreator.environment(World.Environment.THE_END);
        endCreator.seed(resolvedSeed);


        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            overworldCreator.createWorld();
            netherCreator.createWorld();
            endCreator.createWorld();

            plugin.getLogger().info("World reset completed with seed: " + resolvedSeed);


            Bukkit.getScheduler().runTaskLater(plugin, Bukkit::restart, 20L);
        }, 20L);
    }

    public boolean deleteConfiguredWorlds(boolean unloadLoadedWorlds) {
        List<String> worldsToDelete = plugin.getWorldsToDeleteOnReset();
        if (worldsToDelete == null || worldsToDelete.isEmpty()) {
            worldsToDelete = List.of("world", "world_nether", "world_the_end");
        }


        File worldContainer = Bukkit.getWorldContainer();
        File[] files = worldContainer.listFiles();
        if (files == null) {
            plugin.getLogger().warning("Could not list files in world container");
            return false;
        }


        List<String> allWorldNames = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory() && isWorldFolder(file)) {
                allWorldNames.add(file.getName());
            }
        }


        Set<String> uniqueTargets = new LinkedHashSet<>();
        for (String pattern : worldsToDelete) {
            if (pattern.contains("*")) {

                plugin.getLogger().info("Processing wildcard pattern: " + pattern);
                for (String worldName : allWorldNames) {
                    if (matchesPattern(worldName, pattern)) {
                        plugin.getLogger().info("Pattern '" + pattern + "' matches world: " + worldName);
                        uniqueTargets.add(worldName);
                    }
                }
            } else {
                uniqueTargets.add(pattern);
            }
        }

        boolean allSuccessful = true;
        for (String worldName : uniqueTargets) {
            if (unloadLoadedWorlds) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    if (!world.getPlayers().isEmpty()) {
                        world.getPlayers().forEach(player -> player.kick(
                                Component.text("World reset in progress, please reconnect in a moment.").color(
                                        NamedTextColor.RED)));
                    }

                    boolean unloaded = Bukkit.unloadWorld(world, false);
                    if (!unloaded) {
                        plugin.getLogger().warning(
                                "Failed to unload world '" + worldName + "'. Skipping deletion for this world.");
                        allSuccessful = false;
                        continue;
                    }
                }
            }

            if (!deleteWorld(worldName)) {
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }

    private boolean matchesPattern(String worldName, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }


        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");

        return worldName.matches(regex);
    }

    private boolean isWorldFolder(File folder) {
        File levelDat = new File(folder, "level.dat");
        return levelDat.exists() && levelDat.isFile();
    }

    private boolean deleteWorld(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists() && worldFolder.isDirectory()) {
            boolean deleted = deleteDirectory(worldFolder.toPath());
            if (deleted && !worldFolder.exists()) {
                plugin.getLogger().info("Deleted world folder: " + worldName);
                return true;
            }

            plugin.getLogger().warning("Failed to delete world folder: " + worldName + " (folder still exists)");
            return false;
        }

        return true;
    }

    private boolean deleteDirectory(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult postVisitDirectory(@NonNull Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }

                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Error while deleting directory '" + directory + "': " + e.getMessage());
            return false;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("timer.admin")) return completions;

        if (args.length == 1) {

            completions.add("<seed>");
            completions.add(String.valueOf(Bukkit.getServer().getWorlds().getFirst().getSeed()));
        }

        return completions;
    }
}
