package de.thecoolcraft11.timer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimerCommand implements CommandExecutor, TabCompleter {
    private final Timer plugin;
    private final TimerManager timerManager;
    private final MultiTimerManager multiTimerManager;

    public TimerCommand(Timer plugin, TimerManager timerManager, MultiTimerManager multiTimerManager) {
        this.plugin = plugin;
        this.timerManager = timerManager;
        this.multiTimerManager = multiTimerManager;
    }


    private static @Nullable String resolveColorToHex(@Nullable String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.matches("#[0-9A-Fa-f]{6}")) return input.toUpperCase();
        return switch (input.toLowerCase()) {
            case "red" -> "#FF0000";
            case "blue" -> "#0000FF";
            case "magenta" -> "#FF00FF";
            case "green" -> "#008000";
            case "yellow" -> "#FFFF00";
            case "cyan", "aqua" -> "#00FFFF";
            case "white" -> "#FFFFFF";
            case "black" -> "#000000";
            case "gray", "grey" -> "#808080";
            case "dark_gray", "darkgray", "dark-grey", "darkgrey" -> "#404040";
            case "orange" -> "#FFA500";
            case "purple" -> "#800080";
            case "pink" -> "#FFC0CB";
            case "lime" -> "#00FF00";
            case "navy" -> "#000080";
            case "maroon" -> "#800000";
            case "olive" -> "#808000";
            case "teal" -> "#008080";
            case "gold" -> "#FFD700";
            default -> null;
        };
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "hide":
                timerManager.setActionbarVisible(false);
                sender.sendMessage(Component.text("Timer hidden from actionbar!").color(NamedTextColor.YELLOW));
                break;

            case "show":
                timerManager.setActionbarVisible(true);
                sender.sendMessage(Component.text("Timer shown on actionbar!").color(NamedTextColor.GREEN));
                break;

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
                    sender.sendMessage(Component.text("Usage: /timer animation <type|color1|color2|speed|duration> <value>").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Types: gradient, wave, pulse, rainbow, still").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Example: /timer animation type wave").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Example: /timer animation color1 #FF0000 (or 'red')").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Example: /timer animation speed 2.0").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Example: /timer animation duration 10").color(NamedTextColor.GRAY));
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "type":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer animation type <gradient|wave|pulse|rainbow|still>").color(NamedTextColor.RED));
                            return true;
                        }
                        String type = args[2].toLowerCase();
                        if (!Arrays.asList("gradient", "wave", "pulse", "rainbow", "still").contains(type)) {
                            sender.sendMessage(Component.text("Invalid type! Use: gradient, wave, pulse, rainbow, or still").color(NamedTextColor.RED));
                            return true;
                        }
                        plugin.getConfig().set("timer.animation.type", type);
                        plugin.saveConfig();
                        sender.sendMessage(Component.text("Animation type set to: " + type).color(NamedTextColor.GREEN));
                        break;

                    case "color1":
                    case "color2":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer animation " + args[1] + " <color>").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Use hex (#FF0000) or name (red, blue, green, etc.)").color(NamedTextColor.GRAY));
                            return true;
                        }
                        String colorInput = args[2];
                        String hexColor = resolveColorToHex(colorInput);
                        if (hexColor == null) {
                            sender.sendMessage(Component.text("Invalid color! Use hex (#FF0000) or name (red)").color(NamedTextColor.RED));
                            return true;
                        }
                        plugin.getConfig().set("timer.animation." + args[1], hexColor);
                        plugin.saveConfig();
                        sender.sendMessage(Component.text("Animation " + args[1] + " set to: " + hexColor).color(NamedTextColor.GREEN));
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

                    case "duration":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer animation duration <ticks>").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Range: 1 to 100 ticks (20 ticks = 1 second)").color(NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("Example: /timer animation duration 10 (0.5 seconds)").color(NamedTextColor.GRAY));
                            return true;
                        }
                        try {
                            int duration = Integer.parseInt(args[2]);
                            if (duration < 1 || duration > 100) {
                                sender.sendMessage(Component.text("Duration must be between 1 and 100 ticks!").color(NamedTextColor.RED));
                                return true;
                            }
                            timerManager.setAnimationDurationTicks(duration);
                            plugin.getConfig().set("timer.animation.duration-ticks", duration);
                            plugin.saveConfig();
                            sender.sendMessage(Component.text("Animation duration set to: " + duration + " ticks").color(NamedTextColor.GREEN));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid number! Use an integer like 10, 20, etc.").color(NamedTextColor.RED));
                        }
                        break;

                    default:
                        sender.sendMessage(Component.text("Unknown animation option! Use: type, color1, color2, speed, or duration").color(NamedTextColor.RED));
                        break;
                }
                break;

            case "create":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /timer create <name> <global|player|team> [target]").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Examples:").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer create mytimer global").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer create personaltimer player " + sender.getName()).color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer create teamtimer team RedTeam").color(NamedTextColor.GRAY));
                    return true;
                }

                String timerName = args[1];
                String typeStr = args[2].toLowerCase();


                if (timerName.equalsIgnoreCase("global")) {
                    sender.sendMessage(Component.text("Cannot use 'global' as timer name - reserved for main timer!").color(NamedTextColor.RED));
                    return true;
                }

                if (multiTimerManager.hasTimer(timerName)) {
                    sender.sendMessage(Component.text("Timer '" + timerName + "' already exists!").color(NamedTextColor.RED));
                    return true;
                }

                MultiTimerManager.TimerType timerType;
                String targetId = null;

                switch (typeStr) {
                    case "global":
                        timerType = MultiTimerManager.TimerType.GLOBAL;
                        break;
                    case "player":
                        timerType = MultiTimerManager.TimerType.PLAYER;
                        if (args.length < 4) {
                            sender.sendMessage(Component.text("Usage: /timer create <name> player <playername>").color(NamedTextColor.RED));
                            return true;
                        }
                        Player targetPlayer = plugin.getServer().getPlayer(args[3]);
                        if (targetPlayer == null) {
                            sender.sendMessage(Component.text("Player '" + args[3] + "' not found!").color(NamedTextColor.RED));
                            return true;
                        }
                        targetId = targetPlayer.getUniqueId().toString();
                        break;
                    case "team":
                        timerType = MultiTimerManager.TimerType.TEAM;
                        if (args.length < 4) {
                            sender.sendMessage(Component.text("Usage: /timer create <name> team <teamname>").color(NamedTextColor.RED));
                            return true;
                        }
                        targetId = args[3];
                        break;
                    default:
                        sender.sendMessage(Component.text("Invalid timer type! Use: global, player, or team").color(NamedTextColor.RED));
                        return true;
                }

                if (multiTimerManager.createTimer(timerName, timerType, targetId)) {
                    sender.sendMessage(Component.text("Timer '" + timerName + "' created successfully!").color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("Type: " + timerType.name()).color(NamedTextColor.GRAY));
                    if (targetId != null) {
                        sender.sendMessage(Component.text("Target: " + (timerType == MultiTimerManager.TimerType.PLAYER ? args[3] : targetId)).color(NamedTextColor.GRAY));
                    }
                    sender.sendMessage(Component.text("Use /timer use " + timerName + " to manage this timer").color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("Failed to create timer!").color(NamedTextColor.RED));
                }
                break;

            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /timer delete <name>").color(NamedTextColor.RED));
                    return true;
                }

                String deleteTimerName = args[1];
                if (multiTimerManager.deleteTimer(deleteTimerName)) {
                    sender.sendMessage(Component.text("Timer '" + deleteTimerName + "' deleted!").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Timer '" + deleteTimerName + "' not found!").color(NamedTextColor.RED));
                }
                break;

            case "list":
                if (multiTimerManager.getAllTimers().isEmpty()) {
                    sender.sendMessage(Component.text("No custom timers created.").color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("===== Custom Timers =====").color(NamedTextColor.GOLD));
                    for (TimerInstance timer : multiTimerManager.getAllTimers()) {
                        String status = timer.isRunning() ? "Running" : "Stopped";
                        sender.sendMessage(Component.text("• " + timer.getName() + ": ").color(NamedTextColor.YELLOW)
                                .append(Component.text(timer.formatTime()).color(NamedTextColor.WHITE))
                                .append(Component.text(" [" + status + "] ").color(timer.isRunning() ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                                .append(Component.text("(" + timer.getType().name() + ")").color(NamedTextColor.DARK_GRAY)));
                    }
                }
                break;

            case "use":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /timer use <name> <start|stop|pause|resume|reset|set>").color(NamedTextColor.RED));
                    return true;
                }

                String useTimerName = args[1];
                TimerInstance useTimer = multiTimerManager.getTimer(useTimerName);

                if (useTimer == null) {
                    sender.sendMessage(Component.text("Timer '" + useTimerName + "' not found!").color(NamedTextColor.RED));
                    return true;
                }

                String action = args[2].toLowerCase();
                switch (action) {
                    case "start":
                        useTimer.start();
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' started!").color(NamedTextColor.GREEN));
                        break;
                    case "stop":
                        useTimer.stop();
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' stopped!").color(NamedTextColor.RED));
                        break;
                    case "pause":
                        useTimer.pause();
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' paused!").color(NamedTextColor.YELLOW));
                        break;
                    case "resume":
                        useTimer.resume();
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' resumed!").color(NamedTextColor.GREEN));
                        break;
                    case "reset":
                        useTimer.reset();
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' reset!").color(NamedTextColor.YELLOW));
                        break;
                    case "set":
                        if (args.length < 4) {
                            sender.sendMessage(Component.text("Usage: /timer use " + useTimerName + " set <time>").color(NamedTextColor.RED));
                            return true;
                        }
                        try {
                            long setSeconds = parseTime(args[3]);
                            useTimer.animateToTime(setSeconds);
                            sender.sendMessage(Component.text("Timer '" + useTimerName + "' set to " + setSeconds + " seconds").color(NamedTextColor.GREEN));
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(Component.text("Invalid time format!").color(NamedTextColor.RED));
                        }
                        break;
                    case "show":
                        useTimer.setVisible(true);
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' is now visible!").color(NamedTextColor.GREEN));
                        break;
                    case "hide":
                        useTimer.setVisible(false);
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' is now hidden!").color(NamedTextColor.YELLOW));
                        break;
                    case "showname":
                        useTimer.setShowName(true);
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' will now show its name!").color(NamedTextColor.GREEN));
                        break;
                    case "hidename":
                        useTimer.setShowName(false);
                        sender.sendMessage(Component.text("Timer '" + useTimerName + "' name is now hidden!").color(NamedTextColor.YELLOW));
                        break;
                    case "animation":
                        handleAnimationCommand(sender, useTimer, useTimerName, args);
                        break;
                    case "maxtime":
                        handleMaxTimeCommand(sender, useTimer, useTimerName, args);
                        break;
                    case "target":
                        handleTargetCommand(sender, useTimer, useTimerName, args);
                        break;
                    case "info":
                        showTimerInfo(sender, useTimer, useTimerName);
                        break;
                    case "mode":
                        if (args.length < 4) {
                            sender.sendMessage(Component.text("Usage: /timer use " + useTimerName + " mode <up|down>").color(NamedTextColor.RED));
                            return true;
                        }
                        String mode = args[3].toLowerCase();
                        if (mode.equals("up")) {
                            useTimer.setCountingUp(true);
                            sender.sendMessage(Component.text("Timer '" + useTimerName + "' set to count UP").color(NamedTextColor.GREEN));
                        } else if (mode.equals("down")) {
                            useTimer.setCountingUp(false);
                            sender.sendMessage(Component.text("Timer '" + useTimerName + "' set to count DOWN").color(NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Use 'up' or 'down'").color(NamedTextColor.RED));
                        }
                        break;
                    default:
                        sender.sendMessage(Component.text("Unknown action! Use: start, stop, pause, resume, reset, set, show, hide, showname, hidename, animation, maxtime, or target").color(NamedTextColor.RED));
                        break;
                }
                break;

            case "help":
                sendHelp(sender);
                break;

            case "maxtime":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /timer maxtime <set <time>|show|hide|target <add|remove|list|clear>>").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Examples:").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer maxtime set 5m").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer maxtime show").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer maxtime hide").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("  /timer maxtime target add say Time's up!").color(NamedTextColor.GRAY));
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "set":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer maxtime set <time>").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Example: /timer maxtime set 1h30m or /timer maxtime set 90").color(NamedTextColor.GRAY));
                            return true;
                        }
                        try {
                            long maxSeconds = parseTime(args[2]);
                            timerManager.setMaxTime(maxSeconds);
                            sender.sendMessage(Component.text("Max time set to " + maxSeconds + " seconds").color(NamedTextColor.GREEN));
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(Component.text("Invalid time format! Use format like: 1h30m or 90").color(NamedTextColor.RED));
                        }
                        break;

                    case "show":
                        timerManager.setMaxTimeVisible(true);
                        sender.sendMessage(Component.text("Max time is now visible in the timer display!").color(NamedTextColor.GREEN));
                        break;

                    case "hide":
                        timerManager.setMaxTimeVisible(false);
                        sender.sendMessage(Component.text("Max time is now hidden from the timer display!").color(NamedTextColor.GREEN));
                        break;

                    case "target":
                        if (args.length < 3) {
                            sender.sendMessage(Component.text("Usage: /timer maxtime target <add|remove|list|clear> [command]").color(NamedTextColor.RED));
                            sender.sendMessage(Component.text("Examples:").color(NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("  /timer maxtime target add say Time limit reached!").color(NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("  /timer maxtime target remove").color(NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("  /timer maxtime target list").color(NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("  /timer maxtime target clear").color(NamedTextColor.GRAY));
                            return true;
                        }

                        switch (args[2].toLowerCase()) {
                            case "add":
                                if (args.length < 4) {
                                    sender.sendMessage(Component.text("Usage: /timer maxtime target add <command>").color(NamedTextColor.RED));
                                    sender.sendMessage(Component.text("Example: /timer maxtime target add say Time's up!").color(NamedTextColor.GRAY));
                                    return true;
                                }
                                String maxCommand = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                                timerManager.setMaxTarget(maxCommand);
                                sender.sendMessage(Component.text("Max time target set to: ").color(NamedTextColor.GREEN)
                                        .append(Component.text(maxCommand).color(NamedTextColor.WHITE)));
                                break;

                            case "remove":
                                timerManager.removeMaxTarget();
                                sender.sendMessage(Component.text("Max time target removed!").color(NamedTextColor.GREEN));
                                break;

                            case "list":
                                String maxCmd = timerManager.getMaxTargetCommand();
                                if (maxCmd == null || maxCmd.isEmpty()) {
                                    sender.sendMessage(Component.text("No max time target configured.").color(NamedTextColor.YELLOW));
                                } else {
                                    sender.sendMessage(Component.text("Max Time Target: ").color(NamedTextColor.GOLD)
                                            .append(Component.text(maxCmd).color(NamedTextColor.WHITE)));
                                }
                                break;

                            case "clear":
                                timerManager.removeMaxTarget();
                                sender.sendMessage(Component.text("Max time target cleared!").color(NamedTextColor.YELLOW));
                                break;

                            default:
                                sender.sendMessage(Component.text("Unknown maxtime target subcommand! Use: add, remove, list, or clear").color(NamedTextColor.RED));
                                break;
                        }
                        break;

                    default:
                        sender.sendMessage(Component.text("Unknown maxtime subcommand! Use: set, show, hide, or target").color(NamedTextColor.RED));
                        break;
                }
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
        sender.sendMessage(Component.text("--- Multi-Timer Commands ---").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/timer create <name> <global|player|team> [target]").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Create new timer").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer delete <name>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Delete a custom timer").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - List all custom timers").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer use <name> <start|stop|pause|resume|reset|set>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Control custom timer").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("--- Global Timer Commands ---").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/timer hide").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Hide the timer from the actionbar").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer show").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show the timer on the actionbar").color(NamedTextColor.WHITE)));
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
        sender.sendMessage(Component.text("/timer animation duration <ticks>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set animation transition duration (1-100 ticks)").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer maxtime set <time>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Set max time (e.g., 1h30m)").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer maxtime show").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show max time in timer display (format: current / max)").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer maxtime hide").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Hide max time from timer display").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer maxtime target add <cmd>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Execute command when max time is reached").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer maxtime target remove").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Remove max time target").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer maxtime target list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Show max time target").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/timer maxtime target clear").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Clear max time target").color(NamedTextColor.WHITE)));
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

        sender.sendMessage(Component.text("Max Time: ").color(NamedTextColor.YELLOW)
                .append(Component.text(timerManager.getMaxTime() + " seconds").color(NamedTextColor.WHITE))
                .append(Component.text(" (").color(NamedTextColor.GRAY))
                .append(Component.text(timerManager.isMaxTimeVisible() ? "Visible" : "Hidden").color(timerManager.isMaxTimeVisible() ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .append(Component.text(")").color(NamedTextColor.GRAY)));

        String maxTarget = timerManager.getMaxTargetCommand();
        if (maxTarget == null || maxTarget.isEmpty()) {
            sender.sendMessage(Component.text("Max Time Target: ").color(NamedTextColor.YELLOW)
                    .append(Component.text("None configured").color(NamedTextColor.GRAY)));
        } else {
            sender.sendMessage(Component.text("Max Time Target: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(maxTarget).color(NamedTextColor.WHITE)));
        }
    }

    private void handleAnimationCommand(CommandSender sender, TimerInstance timer, String timerName, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /timer use " + timerName + " animation <type|color1|color2|speed|duration> <value>").color(NamedTextColor.RED));
            return;
        }

        String animSubCmd = args[3].toLowerCase();
        switch (animSubCmd) {
            case "type":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " animation type <gradient|wave|pulse|rainbow|still>").color(NamedTextColor.RED));
                    return;
                }
                String type = args[4].toLowerCase();
                if (!Arrays.asList("gradient", "wave", "pulse", "rainbow", "still").contains(type)) {
                    sender.sendMessage(Component.text("Invalid type! Use: gradient, wave, pulse, rainbow, or still").color(NamedTextColor.RED));
                    return;
                }
                timer.setAnimationType(AnimationType.fromString(type));
                sender.sendMessage(Component.text("Animation type set to: " + type).color(NamedTextColor.GREEN));
                break;

            case "color1":
            case "color2":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " animation " + animSubCmd + " <color>").color(NamedTextColor.RED));
                    return;
                }
                String colorInput = args[4];
                String hexColor = resolveColorToHex(colorInput);
                if (hexColor == null) {
                    sender.sendMessage(Component.text("Invalid color! Use hex (#FF0000) or name (red)").color(NamedTextColor.RED));
                    return;
                }
                if (animSubCmd.equals("color1")) {
                    timer.setColor1(hexColor);
                } else {
                    timer.setColor2(hexColor);
                }
                sender.sendMessage(Component.text("Animation " + animSubCmd + " set to: " + hexColor).color(NamedTextColor.GREEN));
                break;

            case "speed":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " animation speed <multiplier>").color(NamedTextColor.RED));
                    return;
                }
                try {
                    double speed = Double.parseDouble(args[4]);
                    if (speed < 0.1 || speed > 10.0) {
                        sender.sendMessage(Component.text("Speed must be between 0.1 and 10.0!").color(NamedTextColor.RED));
                        return;
                    }
                    timer.setAnimationSpeed(speed);
                    sender.sendMessage(Component.text("Animation speed set to: " + speed + "x").color(NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number! Use a decimal like 1.0, 2.5, etc.").color(NamedTextColor.RED));
                }
                break;

            case "duration":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " animation duration <ticks>").color(NamedTextColor.RED));
                    return;
                }
                try {
                    int duration = Integer.parseInt(args[4]);
                    if (duration < 1 || duration > 100) {
                        sender.sendMessage(Component.text("Duration must be between 1 and 100 ticks!").color(NamedTextColor.RED));
                        return;
                    }
                    timer.setAnimationDurationTicks(duration);
                    sender.sendMessage(Component.text("Animation duration set to: " + duration + " ticks").color(NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number! Use an integer like 10, 20, etc.").color(NamedTextColor.RED));
                }
                break;

            default:
                sender.sendMessage(Component.text("Unknown animation option! Use: type, color1, color2, speed, or duration").color(NamedTextColor.RED));
                break;
        }
    }

    private void handleMaxTimeCommand(CommandSender sender, TimerInstance timer, String timerName, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /timer use " + timerName + " maxtime <set|show|hide|target>").color(NamedTextColor.RED));
            return;
        }

        String maxSubCmd = args[3].toLowerCase();
        switch (maxSubCmd) {
            case "set":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " maxtime set <time>").color(NamedTextColor.RED));
                    return;
                }
                try {
                    long maxSeconds = parseTime(args[4]);
                    timer.setMaxTime(maxSeconds);
                    sender.sendMessage(Component.text("Max time set to " + maxSeconds + " seconds").color(NamedTextColor.GREEN));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Invalid time format! Use format like: 1h30m or 90").color(NamedTextColor.RED));
                }
                break;
            case "show":
                timer.setShowMaxTime(true);
                sender.sendMessage(Component.text("Max time is now visible in the timer display!").color(NamedTextColor.GREEN));
                break;
            case "hide":
                timer.setShowMaxTime(false);
                sender.sendMessage(Component.text("Max time is now hidden from the timer display!").color(NamedTextColor.GREEN));
                break;
            case "target":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " maxtime target <add|remove>").color(NamedTextColor.RED));
                    return;
                }
                String targetAction = args[4].toLowerCase();
                if (targetAction.equals("add")) {
                    if (args.length < 6) {
                        sender.sendMessage(Component.text("Usage: /timer use " + timerName + " maxtime target add <command>").color(NamedTextColor.RED));
                        return;
                    }
                    String command = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                    timer.setMaxTargetCommand(command);
                    sender.sendMessage(Component.text("Max time target set to: " + command).color(NamedTextColor.GREEN));
                } else if (targetAction.equals("remove")) {
                    timer.setMaxTargetCommand(null);
                    sender.sendMessage(Component.text("Max time target removed!").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Use 'add' or 'remove'").color(NamedTextColor.RED));
                }
                break;
            default:
                sender.sendMessage(Component.text("Unknown maxtime option! Use: set, show, hide, or target").color(NamedTextColor.RED));
                break;
        }
    }

    private void handleTargetCommand(CommandSender sender, TimerInstance timer, String timerName, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /timer use " + timerName + " target <add|remove|list|clear>").color(NamedTextColor.RED));
            return;
        }

        String targetSubCmd = args[3].toLowerCase();
        switch (targetSubCmd) {
            case "add":
                if (args.length < 6) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " target add <id> <time> [command]").color(NamedTextColor.RED));
                    return;
                }
                String targetId = args[4];
                try {
                    long targetTime = parseTime(args[5]);
                    String targetCommand = args.length > 6 ? String.join(" ", Arrays.copyOfRange(args, 6, args.length)) : null;
                    timer.addTarget(targetId, targetTime, targetCommand);
                    sender.sendMessage(Component.text("Target '" + targetId + "' added at " + targetTime + " seconds").color(NamedTextColor.GREEN));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Invalid time format!").color(NamedTextColor.RED));
                }
                break;
            case "remove":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /timer use " + timerName + " target remove <id>").color(NamedTextColor.RED));
                    return;
                }
                String removeId = args[4];
                if (timer.removeTarget(removeId)) {
                    sender.sendMessage(Component.text("Target '" + removeId + "' removed!").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Target '" + removeId + "' not found!").color(NamedTextColor.RED));
                }
                break;
            case "list":
                if (timer.getAllTargets().isEmpty()) {
                    sender.sendMessage(Component.text("No targets configured for timer '" + timerName + "'").color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("Targets for '" + timerName + "':").color(NamedTextColor.GOLD));
                    for (TimerTarget target : timer.getAllTargets().values()) {
                        String status = target.isExecuted() ? " [EXECUTED]" : "";
                        sender.sendMessage(Component.text("  • " + target.getId() + ": ").color(NamedTextColor.GRAY)
                                .append(Component.text(target.getTime() + "s").color(NamedTextColor.WHITE))
                                .append(Component.text(target.getCommand() != null ? " → " + target.getCommand() : "").color(NamedTextColor.DARK_GRAY))
                                .append(Component.text(status).color(NamedTextColor.DARK_GRAY)));
                    }
                }
                break;
            case "clear":
                timer.clearAllTargets();
                sender.sendMessage(Component.text("All targets cleared for timer '" + timerName + "'!").color(NamedTextColor.YELLOW));
                break;
            default:
                sender.sendMessage(Component.text("Unknown target option! Use: add, remove, list, or clear").color(NamedTextColor.RED));
                break;
        }
    }

    private void showTimerInfo(CommandSender sender, TimerInstance timer, String timerName) {
        sender.sendMessage(Component.text("===== Timer Info: " + timerName + " =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Type: ").color(NamedTextColor.YELLOW).append(Component.text(timer.getType().name()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Current Time: ").color(NamedTextColor.YELLOW).append(Component.text(timer.formatTime()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Status: ").color(NamedTextColor.YELLOW).append(Component.text(timer.isRunning() ? "Running" : "Stopped").color(timer.isRunning() ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("Mode: ").color(NamedTextColor.YELLOW).append(Component.text(timer.isCountingUp() ? "Count UP" : "Count DOWN").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Visible: ").color(NamedTextColor.YELLOW).append(Component.text(timer.isVisible() ? "Yes" : "No").color(timer.isVisible() ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("Show Name: ").color(NamedTextColor.YELLOW).append(Component.text(timer.isShowName() ? "Yes" : "No").color(timer.isShowName() ? NamedTextColor.GREEN : NamedTextColor.GRAY)));

        sender.sendMessage(Component.text("Animation: ").color(NamedTextColor.YELLOW).append(Component.text(timer.getAnimationType().toString()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Color 1: ").color(NamedTextColor.GRAY).append(Component.text(timer.getColor1()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Color 2: ").color(NamedTextColor.GRAY).append(Component.text(timer.getColor2()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Speed: ").color(NamedTextColor.GRAY).append(Component.text(timer.getAnimationSpeed() + "x").color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("Max Time: ").color(NamedTextColor.YELLOW).append(Component.text(timer.getMaxTime() + " seconds").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Show Max Time: ").color(NamedTextColor.YELLOW).append(Component.text(timer.isShowMaxTime() ? "Yes" : "No").color(timer.isShowMaxTime() ? NamedTextColor.GREEN : NamedTextColor.GRAY)));

        if (timer.getAllTargets().isEmpty()) {
            sender.sendMessage(Component.text("Targets: ").color(NamedTextColor.YELLOW).append(Component.text("None").color(NamedTextColor.GRAY)));
        } else {
            sender.sendMessage(Component.text("Targets: " + timer.getAllTargets().size()).color(NamedTextColor.YELLOW));
            for (TimerTarget target : timer.getAllTargets().values()) {
                String status = target.isExecuted() ? " [EXECUTED]" : "";
                sender.sendMessage(Component.text("  • " + target.getId() + ": ").color(NamedTextColor.GRAY)
                        .append(Component.text(target.getTime() + "s").color(NamedTextColor.WHITE))
                        .append(Component.text(target.getCommand() != null ? " → " + target.getCommand() : "").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(status).color(NamedTextColor.DARK_GRAY)));
            }
        }
    }


    private List<String> getTimeSuggestions() {
        return Arrays.asList("30", "60", "90", "5m", "10m", "15m", "30m", "45m", "1h", "2h", "5h");
    }

    private List<String> getAnimationTypeSuggestions() {
        return Arrays.asList("gradient", "wave", "pulse", "rainbow", "still");
    }

    private List<String> getColorSuggestions() {
        return Arrays.asList("red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple", "pink", "white", "black", "gray",
                "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF", "#FF00FF", "#FF6600", "#FF0099", "#00FF99");
    }

    private List<String> getSpeedSuggestions() {
        return Arrays.asList("0.5", "1.0", "1.5", "2.0", "3.0", "5.0", "10.0");
    }

    private List<String> getDurationSuggestions() {
        return Arrays.asList("5", "10", "15", "20", "30", "50", "100");
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
                    "set", "mode", "target", "animation", "anim", "maxtime", "create", "delete", "list", "use", "hide", "show", "info", "save", "reload", "help");
            for (String cmd : commands) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {

                completions.add("<timer-name>");
            } else if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("use")) {

                for (TimerInstance timer : multiTimerManager.getAllTimers()) {
                    if (timer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(timer.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("mode")) {
                completions.add("up");
                completions.add("down");
            } else if (args[0].equalsIgnoreCase("set")) {

                for (String time : getTimeSuggestions()) {
                    if (time.startsWith(args[1])) {
                        completions.add(time);
                    }
                }
            } else if (args[0].equalsIgnoreCase("target")) {
                List<String> subcommands = Arrays.asList("add", "remove", "list", "clear");
                for (String sub : subcommands) {
                    if (sub.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim")) {
                List<String> animOptions = Arrays.asList("type", "color1", "color2", "speed", "duration");
                for (String opt : animOptions) {
                    if (opt.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(opt);
                    }
                }
            } else if (args[0].equalsIgnoreCase("maxtime")) {
                List<String> subcommands = Arrays.asList("set", "show", "hide", "target");
                for (String sub : subcommands) {
                    if (sub.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create")) {
                List<String> types = Arrays.asList("global", "player", "team");
                for (String type : types) {
                    if (type.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use")) {
                List<String> actions = Arrays.asList("start", "stop", "pause", "resume", "reset", "set",
                        "show", "hide", "showname", "hidename", "animation", "maxtime", "target", "mode", "info");
                for (String action : actions) {
                    if (action.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(action);
                    }
                }
            } else if (args[0].equalsIgnoreCase("target") && args[1].equalsIgnoreCase("remove")) {

                for (TimerTarget target : timerManager.getAllTargets()) {
                    if (target.getId().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(target.getId());
                    }
                }
            } else if (args[0].equalsIgnoreCase("target") && args[1].equalsIgnoreCase("add")) {

                completions.add("<target-id>");
            } else if ((args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))
                    && args[1].equalsIgnoreCase("type")) {

                for (String type : getAnimationTypeSuggestions()) {
                    if (type.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if ((args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))
                    && (args[1].equalsIgnoreCase("color1") || args[1].equalsIgnoreCase("color2"))) {

                List<String> colorSuggestions = getColorSuggestions();
                for (String s : colorSuggestions) {
                    if (s.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(s);
                    }
                }
            } else if ((args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))
                    && args[1].equalsIgnoreCase("speed")) {

                List<String> speedSuggestions = getSpeedSuggestions();
                for (String s : speedSuggestions) {
                    if (s.startsWith(args[2])) {
                        completions.add(s);
                    }
                }
            } else if ((args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("anim"))
                    && args[1].equalsIgnoreCase("duration")) {

                List<String> durationSuggestions = getDurationSuggestions();
                for (String s : durationSuggestions) {
                    if (s.startsWith(args[2])) {
                        completions.add(s);
                    }
                }
            } else if (args[0].equalsIgnoreCase("maxtime") && args[1].equalsIgnoreCase("target")) {
                List<String> targetSubs = Arrays.asList("add", "remove", "list", "clear");
                for (String sub : targetSubs) {
                    if (sub.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args[0].equalsIgnoreCase("maxtime") && args[1].equalsIgnoreCase("set")) {

                for (String time : getTimeSuggestions()) {
                    if (time.startsWith(args[2])) {
                        completions.add(time);
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("target") && args[1].equalsIgnoreCase("add")) {

                for (String time : getTimeSuggestions()) {
                    if (time.startsWith(args[3])) {
                        completions.add(time);
                    }
                }
            } else if (args[0].equalsIgnoreCase("maxtime") && args[1].equalsIgnoreCase("set")) {

                for (String time : getTimeSuggestions()) {
                    if (time.startsWith(args[3])) {
                        completions.add(time);
                    }
                }
            } else if (args[0].equalsIgnoreCase("create")) {
                String type = args[2].toLowerCase();
                if (type.equals("player")) {

                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(p.getName());
                        }
                    }
                } else if (type.equals("team")) {

                    completions.add("<team-name>");
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("set")) {

                for (String time : getTimeSuggestions()) {
                    if (time.startsWith(args[3])) {
                        completions.add(time);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("animation")) {

                List<String> animOptions = Arrays.asList("type", "color1", "color2", "speed", "duration");
                for (String opt : animOptions) {
                    if (opt.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(opt);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("maxtime")) {

                List<String> maxOptions = Arrays.asList("set", "show", "hide", "target");
                for (String opt : maxOptions) {
                    if (opt.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(opt);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("target")) {

                List<String> targetOptions = Arrays.asList("add", "remove", "list", "clear");
                for (String opt : targetOptions) {
                    if (opt.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(opt);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("mode")) {
                List<String> modes = Arrays.asList("up", "down");
                for (String mode : modes) {
                    if (mode.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(mode);
                    }
                }
            }
        } else if (args.length == 5) {

            if (args[0].equalsIgnoreCase("target") && args[1].equalsIgnoreCase("add")) {

                for (String time : getTimeSuggestions()) {
                    if (time.startsWith(args[4])) {
                        completions.add(time);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("animation") && args[3].equalsIgnoreCase("type")) {
                List<String> animTypes = getAnimationTypeSuggestions();
                for (String type : animTypes) {
                    if (type.toLowerCase().startsWith(args[4].toLowerCase())) {
                        completions.add(type);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("animation") &&
                    (args[3].equalsIgnoreCase("color1") || args[3].equalsIgnoreCase("color2"))) {

                List<String> colors = getColorSuggestions();
                for (String color : colors) {
                    if (color.toLowerCase().startsWith(args[4].toLowerCase())) {
                        completions.add(color);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("animation") && args[3].equalsIgnoreCase("speed")) {

                List<String> speeds = getSpeedSuggestions();
                for (String speed : speeds) {
                    if (speed.startsWith(args[4])) {
                        completions.add(speed);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("animation") && args[3].equalsIgnoreCase("duration")) {

                List<String> durations = getDurationSuggestions();
                for (String duration : durations) {
                    if (duration.startsWith(args[4])) {
                        completions.add(duration);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("mode")) {
                List<String> modes = Arrays.asList("up", "down");
                for (String mode : modes) {
                    if (mode.toLowerCase().startsWith(args[4].toLowerCase())) {
                        completions.add(mode);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("maxtime") && args[3].equalsIgnoreCase("set")) {

                List<String> times = getTimeSuggestions();
                for (String time : times) {
                    if (time.startsWith(args[4])) {
                        completions.add(time);
                    }
                }
            } else if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("target") && args[3].equalsIgnoreCase("add")) {

                if (args[4].length() < 20) {
                    completions.add("<target-id>");
                }
            }
        } else if (args.length == 6) {

            if (args[0].equalsIgnoreCase("use") && args[2].equalsIgnoreCase("target") && args[3].equalsIgnoreCase("add")) {

                List<String> times = getTimeSuggestions();
                for (String time : times) {
                    if (time.startsWith(args[5])) {
                        completions.add(time);
                    }
                }
            }
        }

        return completions;
    }
}
