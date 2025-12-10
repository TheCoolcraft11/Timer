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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ResetCommand implements CommandExecutor, TabCompleter {
    private final Timer plugin;

    public ResetCommand(Timer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("timer.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
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
        Bukkit.broadcast(Component.text("WARNING: The world is being reset" + seedMessage + "!").color(NamedTextColor.RED));
        Bukkit.broadcast(Component.text("All players will be kicked and the server will restart shortly...").color(NamedTextColor.YELLOW));

        final Long finalSeed = seed;


        Bukkit.getScheduler().runTaskLater(plugin, () -> performWorldReset(finalSeed), 60L);

        return true;
    }

    private void performWorldReset(Long seed) {

        if (seed == null) {
            seed = ThreadLocalRandom.current().nextLong();
        }

        plugin.getLogger().info("Resetting world with seed: " + seed);

        deleteConfiguredWorlds();


        WorldCreator overworldCreator = new WorldCreator("world");
        overworldCreator.environment(World.Environment.NORMAL);
        overworldCreator.seed(seed);

        WorldCreator netherCreator = new WorldCreator("world_nether");
        netherCreator.environment(World.Environment.NETHER);
        netherCreator.seed(seed);

        WorldCreator endCreator = new WorldCreator("world_the_end");
        endCreator.environment(World.Environment.THE_END);
        endCreator.seed(seed);


        overworldCreator.createWorld();
        netherCreator.createWorld();
        endCreator.createWorld();

        plugin.getLogger().info("World reset completed with seed: " + seed);


        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::restart, 20L);
    }

    public void deleteConfiguredWorlds() {
        List<String> worldsToDelete = plugin.getWorldsToDeleteOnReset();
        if (worldsToDelete == null || worldsToDelete.isEmpty()) {
            worldsToDelete = List.of("world", "world_nether", "world_the_end");
        }


        File worldContainer = Bukkit.getWorldContainer();
        File[] files = worldContainer.listFiles();
        if (files == null) {
            plugin.getLogger().warning("Could not list files in world container");
            return;
        }


        List<String> allWorldNames = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory() && isWorldFolder(file)) {
                allWorldNames.add(file.getName());
            }
        }


        for (String pattern : worldsToDelete) {
            if (pattern.contains("*")) {

                plugin.getLogger().info("Processing wildcard pattern: " + pattern);
                for (String worldName : allWorldNames) {
                    if (matchesPattern(worldName, pattern)) {
                        plugin.getLogger().info("Pattern '" + pattern + "' matches world: " + worldName);
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Bukkit.unloadWorld(world, false);
                        }
                        deleteWorld(worldName);
                    }
                }
            } else {

                World world = Bukkit.getWorld(pattern);
                if (world != null) {
                    Bukkit.unloadWorld(world, false);
                }
                deleteWorld(pattern);
            }
        }
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

    private void deleteWorld(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists() && worldFolder.isDirectory()) {
            deleteDirectory(worldFolder);
            plugin.getLogger().info("Deleted world folder: " + worldName);
        }
    }

    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
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
