package de.thecoolcraft11.timer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimerCommand implements CommandExecutor, TabCompleter {
    private final Timer plugin;
    private final TimerManager timerManager;

    public TimerCommand(Timer plugin, TimerManager timerManager) {
        this.plugin = plugin;
        this.timerManager = timerManager;
    }

    
    private static @Nullable String resolveColorToHex(@Nullable String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.matches("#[0-9A-Fa-f]{6}")) return input.toUpperCase();
        switch (input.toLowerCase()) {
            case "red": return "#FF0000";
            case "blue": return "#0000FF";
            case "magenta": return "#FF00FF";
            case "green": return "#00FF00";
            case "yellow": return "#FFFF00";
            case "cyan": case "aqua": return "#00FFFF";
            case "white": return "#FFFFFF";
            case "black": return "#000000";
            case "gray": case "grey": return "#808080";
            case "dark_gray": case "darkgray": case "dark-grey": case "darkgrey": return "#404040";
            case "orange": return "#FFA500";
            case "purple": return "#800080";
            case "pink": return "#FFC0CB";
            case "lime": return "#00FF00";
            case "navy": return "#000080";
            case "maroon": return "#800000";
            case "olive": return "#808000";
            case "teal": return "#008080";
            case "gold": return "#FFD700";
            default: return null;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                timerManager.start();
                sender.sendMessage(Component.text("Timer started!").color(NamedTextColor.GREEN));
                break;

            case "stop":
                timerManager.stop();
                sender.sendMessage(Component.text("Timer stopped!").color(NamedTextColor.RED));
                break;

            case "pause":
                timerManager.pause();
                sender.sendMessage(Component.text("Timer paused!").color(NamedTextColor.YELLOW));
                break;

            case "resume":
                timerManager.resume();
                sender.sendMessage(Component.text("Timer resumed!").color(NamedTextColor.GREEN));
                break;

            case "reset":
                timerManager.reset();
                sender.sendMessage(Component.text("Timer reset!").color(NamedTextColor.YELLOW));
                break;

            case "set":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /timer set <time>").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Example: /timer set 1h30m or /timer set 90 (seconds)").color(NamedTextColor.GRAY));
                    return true;
                }
                try {
                    long seconds = parseTime(args[1]);
                    timerManager.setTime(seconds);
                    sender.sendMessage(Component.text("Set timer to " + seconds + " seconds...").color(NamedTextColor.GREEN));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Invalid time format! Use format like: 1h30m or 90").color(NamedTextColor.RED));
                }
                break;

            case "mode":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /timer mode <up|down>").color(NamedTextColor.RED));
                    return true;
                }
                if (args[1].equalsIgnoreCase("up")) {
                    timerManager.setCountingUp(true);
                    sender.sendMessage(Component.text("Timer mode set to COUNT UP!").color(NamedTextColor.GREEN));
                } else if (args[1].equalsIgnoreCase("down")) {
                    timerManager.setCountingUp(false);
                    sender.sendMessage(Component.text("Timer mode set to COUNT DOWN!").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Invalid mode! Use 'up' or 'down'").color(NamedTextColor.RED));
                }
                break;

            case "target":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /timer target <add|remove|list|clear> [args...]").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Examples:").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer target add <id> <time> [command]").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer target remove <id>").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer target list").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer target clear").color(NamedTextColor.GRAY));
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "add":
                        if (args.length < 4) {
                            sender.sendMessage(Component.text("Usage: /timer target add <id> <time> [command]").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Example: /timer target add warning1 5m say 5 minutes remaining!").color(NamedTextColor.GRAY));
                            return true;
                        }
                        try {
                            String id = args[2];
                            long targetTime = parseTime(args[3]);
                            String targetCommand = createTargetCommand(args);

                            timerManager.addTarget(id, targetTime, targetCommand);
                            sender.sendMessage(Component.text("Target '" + id + "' added at " + targetTime + " seconds" +
                                    (targetCommand != null ? " with command: " + targetCommand : "")).color(NamedTextColor.GREEN));
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(Component.text("Invalid time format! Use format like: 1h30m or 90").color(NamedTextColor.RED));
                        }
                        break;

                    case "remove":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer target remove <id>").color(NamedTextColor.RED));
                            return true;
                        }
                        String removeId = args[2];
                        if (timerManager.removeTarget(removeId)) {
                            sender.sendMessage(Component.text("Target '" + removeId + "' removed!").color(NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Target '" + removeId + "' not found!").color(NamedTextColor.RED));
                        }
                        break;

                    case "list":
                        if (timerManager.hasNoTargets()) {
                            sender.sendMessage(Component.text("No targets configured.").color(NamedTextColor.YELLOW));
                        } else {
                            sender.sendMessage(Component.text("===== Configured Targets =====").color(NamedTextColor.GOLD));
                            for (TimerTarget target : timerManager.getAllTargets()) {
                                String status = target.isExecuted() ? " [EXECUTED]" : "";
                                sender.sendMessage(Component.text("• " + target.getId() + ": ").color(NamedTextColor.YELLOW)
                                        .append(Component.text(target.getTime() + "s").color(NamedTextColor.WHITE))
                                        .append(Component.text(target.getCommand() != null ? " → " + target.getCommand() : "").color(NamedTextColor.GRAY))
                                        .append(Component.text(status).color(NamedTextColor.DARK_GRAY)));
                            }
                        }
                        break;

                    case "clear":
                        timerManager.clearAllTargets();
                        sender.sendMessage(Component.text("All targets cleared!").color(NamedTextColor.YELLOW));
                        break;

                    default:
                        sender.sendMessage(Component.text("Unknown target subcommand! Use: add, remove, list, or clear").color(NamedTextColor.RED));
                        break;
                }
                break;

            case "info":
                showInfo(sender);
                break;

            case "save":
                timerManager.saveToConfig();
                sender.sendMessage(Component.text("Timer configuration saved!").color(NamedTextColor.GREEN));
                break;

            case "reload":
                timerManager.loadFromConfig();
                sender.sendMessage(Component.text("Timer configuration reloaded!").color(NamedTextColor.GREEN));
                break;

            case "animation":
            case "anim":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /timer animation <type|color1|color2|speed> <value>").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Types: gradient, wave, pulse, rainbow, still").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Example: /timer animation type wave").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Example: /timer animation color1 #FF0000 (or 'red')").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Example: /timer animation speed 2.0").color(NamedTextColor.GRAY));
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "type":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer animation type <gradient|wave|pulse|rainbow|still>").color(NamedTextColor.RED));
                            return true;
                        }
                        String type = args[2].toLowerCase();
                        if (!type.equals("gradient") && !type.equals("wave") && !type.equals("pulse")
                                && !type.equals("rainbow") && !type.equals("still")) {
                            sender.sendMessage(Component.text("Invalid animation type! Use: gradient, wave, pulse, rainbow, or still").color(NamedTextColor.RED));
                            return true;
                        }
                        plugin.getConfig().set("timer.animation.type", type);
                        plugin.saveConfig();
                        sender.sendMessage(Component.text("Animation type set to: " + type).color(NamedTextColor.GREEN));
                        break;

                    case "color1":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer animation color1 <hex|name>").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Example: /timer animation color1 #FF0000 or /timer animation color1 red").color(NamedTextColor.GRAY));
                            return true;
                        }
                        String color1 = args[2];
                        String resolved1 = resolveColorToHex(color1);
                        if (resolved1 == null) {
                            sender.sendMessage(Component.text("Invalid color! Use hex: #RRGGBB or a name like 'red', 'blue', 'magenta'").color(NamedTextColor.RED));
                            return true;
                        }
                        plugin.getConfig().set("timer.animation.color1", resolved1);
                        plugin.saveConfig();
                        sender.sendMessage(Component.text("Color 1 set to: " + resolved1).color(NamedTextColor.GREEN));
                        break;

                    case "color2":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer animation color2 <hex|name>").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Example: /timer animation color2 #0000FF or /timer animation color2 blue").color(NamedTextColor.GRAY));
                            return true;
                        }
                        String color2 = args[2];
                        String resolved2 = resolveColorToHex(color2);
                        if (resolved2 == null) {
                            sender.sendMessage(Component.text("Invalid color! Use hex: #RRGGBB or a name like 'red', 'blue', 'magenta'").color(NamedTextColor.RED));
                            return true;
                        }
                        plugin.getConfig().set("timer.animation.color2", resolved2);
                        plugin.saveConfig();
                        sender.sendMessage(Component.text("Color 2 set to: " + resolved2).color(NamedTextColor.GREEN));
                        break;

                    case "speed":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer animation speed <multiplier>").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Range: 0.1 to 10.0 (default: 1.0)").color(NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("Example: /timer animation speed 2.0 (double speed)").color(NamedTextColor.GRAY));
                            return true;
                        }
                        try {
                            double speed = Double.parseDouble(args[2]);
                            if (speed < 0.1 || speed > 10.0) {
                                sender.sendMessage(Component.text("Speed must be between 0.1 and 10.0!").color(NamedTextColor.RED));
                                return true;
                            }
                            timerManager.setAnimationSpeed(speed);
                            plugin.getConfig().set("timer.animation.speed", speed);
                            plugin.saveConfig();
                            sender.sendMessage(Component.text("Animation speed set to: " + speed + "x").color(NamedTextColor.GREEN));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid number! Use a decimal like 1.0, 2.5, etc.").color(NamedTextColor.RED));
                        }
                        break;

                    default:
                        sender.sendMessage(Component.text("Unknown animation option! Use: type, color1, color2, or speed").color(NamedTextColor.RED));
                        break;
                }
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(Component.text("Unknown command! Use /timer help for help.").color(NamedTextColor.RED));
                break;
        }

        return true;
    }

    private static @Nullable String createTargetCommand(@NotNull String @NotNull [] args) {
        String targetCommand = null;

        if (args.length > 4) {
            StringBuilder commandBuilder = new StringBuilder();
            for (int i = 4; i < args.length; i++) {
                if (i > 4) commandBuilder.append(" ");
                commandBuilder.append(args[i]);
            }
            targetCommand = commandBuilder.toString();
        }
        return targetCommand;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("===== Timer Plugin Help =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/timer start").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Start the timer").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer stop").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Stop the timer").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer pause").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Pause the timer").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer resume").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Resume the timer").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer reset").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Reset the timer to 0").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer set <time>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set timer (e.g., 1h30m or 90)").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer mode <up|down>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set counting direction").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer target add <id> <time> [cmd]").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Add target").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer target remove <id>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Remove target").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer target list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - List all targets").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer target clear").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Clear all targets").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer animation type <style>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set animation style").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer animation color1 <hex|name>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set primary color (hex like #FF0000 or name like 'red')").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer animation color2 <hex|name>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set secondary color").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer animation speed <multiplier>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set animation speed (0.1-10.0)").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer info").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show timer information").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer save").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Save configuration").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer reload").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Reload configuration").color(NamedTextColor.WHITE)));
    }

    private void showInfo(CommandSender sender) {
        sender.sendMessage(Component.text("===== Timer Information =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Status: ").color(NamedTextColor.YELLOW)
                .append(Component.text(timerManager.isRunning() ? "Running" : "Stopped").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Current Time: ").color(NamedTextColor.YELLOW)
                .append(Component.text(timerManager.getCurrentTime() + " seconds").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Mode: ").color(NamedTextColor.YELLOW)
                .append(Component.text(timerManager.isCountingUp() ? "Count Up" : "Count Down").color(NamedTextColor.WHITE)));

        if (timerManager.hasNoTargets()) {
            sender.sendMessage(Component.text("Targets: ").color(NamedTextColor.YELLOW)
                    .append(Component.text("None configured").color(NamedTextColor.GRAY)));
        } else {
            sender.sendMessage(Component.text("Targets: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(timerManager.getAllTargets().size() + " configured").color(NamedTextColor.WHITE)));
            for (TimerTarget target : timerManager.getAllTargets()) {
                String status = target.isExecuted() ? " [EXECUTED]" : "";
                sender.sendMessage(Component.text("  • " + target.getId() + ": ").color(NamedTextColor.GRAY)
                        .append(Component.text(target.getTime() + "s").color(NamedTextColor.WHITE))
                        .append(Component.text(target.getCommand() != null ? " → " + target.getCommand() : "").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(status).color(NamedTextColor.DARK_GRAY)));
            }
        }
    }

    private long parseTime(String timeStr) throws IllegalArgumentException {
        timeStr = timeStr.toLowerCase().trim();
        long totalSeconds = 0;

        
        try {
            return Long.parseLong(timeStr);
        } catch (NumberFormatException ignored) {
            
        }

        
        StringBuilder currentNum = new StringBuilder();

        for (char c : timeStr.toCharArray()) {
            if (Character.isDigit(c)) {
                currentNum.append(c);
            } else if (c == 'h' || c == 'm' || c == 's') {
                if (!currentNum.isEmpty()) {
                    long value = Long.parseLong(currentNum.toString());
                    switch (c) {
                        case 'h':
                            totalSeconds += value * 3600;
                            break;
                        case 'm':
                            totalSeconds += value * 60;
                            break;
                        case 's':
                            totalSeconds += value;
                            break;
                    }
                    currentNum = new StringBuilder();
                }
            }
        }

        if (totalSeconds == 0) {
            throw new IllegalArgumentException("Invalid time format");
        }

        return totalSeconds;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = Arrays.asList("start", "stop", "pause", "resume", "reset",
                    "set", "mode", "target", "animation", "anim", "info", "save", "reload", "help");
            for (String cmd : commands) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("mode")) {
                completions.add("up");
                completions.add("down");
            } else if (args[0].equalsIgnoreCase("set")) {
                completions.add("60");
                completions.add("5m");
                completions.add("1h");
                completions.add("1h30m");
            } else if (args[0].equalsIgnoreCase("target")) {
                List<String> subcommands = Arrays.asList("add", "remove", "list", "clear");
                for (String sub : subcommands) {
                    if (sub.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim")) {
                List<String> animOptions = Arrays.asList("type", "color1", "color2", "speed");
                for (String opt : animOptions) {
                    if (opt.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(opt);
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("target") && args[1].equalsIgnoreCase("remove")) {
                
                for (TimerTarget target : timerManager.getAllTargets()) {
                    if (target.getId().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(target.getId());
                    }
                }
            } else if ((args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))
                    && args[1].equalsIgnoreCase("type")) {
                List<String> types = Arrays.asList("gradient", "wave", "pulse", "rainbow", "still");
                for (String type : types) {
                    if (type.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if ((args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))
                    && (args[1].equalsIgnoreCase("color1") || args[1].equalsIgnoreCase("color2"))) {
                
                List<String> colorSuggestions = Arrays.asList("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF",
                        "red", "green", "blue", "yellow", "magenta", "cyan", "white", "black", "orange", "purple", "pink", "gold");
                for (String s : colorSuggestions) {
                    if (s.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(s);
                    }
                }
            } else if ((args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))
                    && args[1].equalsIgnoreCase("speed")) {
                
                List<String> speedSuggestions = Arrays.asList("0.5", "1.0", "1.5", "2.0", "3.0", "5.0");
                for (String s : speedSuggestions) {
                    if (s.startsWith(args[2])) {
                        completions.add(s);
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("target") && args[1].equalsIgnoreCase("add")) {
                completions.add("60");
                completions.add("5m");
                completions.add("1h");
                completions.add("1h30m");
            }
        }

        return completions;
    }
}
