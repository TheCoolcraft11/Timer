package de.thecoolcraft11.timer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            sender.sendMessage(
                    Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return true;
        }

        Long seed = null;
        if (args.length > 0) {
            if (plugin.isCustomSeedDisallowed()) {
                sender.sendMessage(
                        Component.text("Custom seeds are disabled in the config!").color(NamedTextColor.RED));
                return true;
            }

            try {
                seed = Long.parseLong(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid seed! Must be a number.").color(NamedTextColor.RED));
                return true;
            }
        }

        if (seed == null) {
            seed = ThreadLocalRandom.current().nextLong();
        }
        final long resolvedSeed = seed;

        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.prepareWorldReset(resolvedSeed), 60L);

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("timer.admin")) {
            return completions;
        }

        if (args.length == 1) {
            if (!plugin.isCustomSeedDisallowed()) {
                completions.add("<seed>");

                if (!plugin.isSeedHidden() && !Bukkit.getWorlds().isEmpty()) {
                    completions.add(String.valueOf(Bukkit.getWorlds().getFirst().getSeed()));
                }
            }
        }

        return completions;
    }
}
