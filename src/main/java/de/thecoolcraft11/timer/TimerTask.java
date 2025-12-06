package de.thecoolcraft11.timer;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Objects;

public class TimerTask extends BukkitRunnable {
    private final TimerManager timerManager;
    private final MultiTimerManager multiTimerManager;

    public TimerTask(TimerManager timerManager, MultiTimerManager multiTimerManager) {
        this.timerManager = timerManager;
        this.multiTimerManager = multiTimerManager;
    }

    @Override
    public void run() {

        timerManager.tick();
        multiTimerManager.tick();

        
        for (TimerInstance timer : multiTimerManager.getAllTimers()) {
            if (timer.isRunning()) {
                
                for (TimerTarget target : timer.getAllTargets().values()) {
                    if (!target.isExecuted() && timer.getCurrentTime() == target.getTime()) {
                        executeCommand(target.getCommand());
                        target.setExecuted(true);
                    }
                }

                
                if (timer.getMaxTime() > 0 && timer.getCurrentTime() >= timer.getMaxTime()) {
                    String maxCmd = timer.getMaxTargetCommand();
                    if (maxCmd != null && !maxCmd.isEmpty()) {
                        executeCommand(maxCmd);
                        timer.setMaxTargetCommand(null); 
                    }
                }
            }
        }


        for (Player player : Bukkit.getOnlinePlayers()) {
            Component displayText = Component.empty();

            
            if (timerManager.isActionbarVisible()) {
                displayText = timerManager.getDisplayText();
            }

            
            List<TimerInstance> globalTimers = multiTimerManager.getGlobalTimers();
            for (TimerInstance timer : globalTimers) {
                if (timer.isVisible()) {
                    if (!displayText.equals(Component.empty())) {
                        displayText = displayText.append(Component.text("  "));
                    }
                    displayText = displayText.append(timer.getDisplayComponent());
                }
            }

            
            List<TimerInstance> playerTimers = multiTimerManager.getTimersForPlayer(player);
            for (TimerInstance timer : playerTimers) {
                if (timer.isVisible()) {
                    if (!displayText.equals(Component.empty())) {
                        displayText = displayText.append(Component.text("  "));
                    }
                    displayText = displayText.append(timer.getDisplayComponent());
                }
            }


            player.getScoreboard();
            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team != null) {
                List<TimerInstance> teamTimers = multiTimerManager.getTimersForTeam(team.getName());
                for (TimerInstance timer : teamTimers) {
                    if (timer.isVisible()) {
                        if (!displayText.equals(Component.empty())) {
                            displayText = displayText.append(Component.text("  "));
                        }
                        displayText = displayText.append(timer.getDisplayComponent());
                    }
                }
            }

            if (!displayText.equals(Component.empty())) {
                player.sendActionBar(displayText);
            }
        }
    }

    private void executeCommand(String command) {
        if (command != null && !command.isEmpty()) {
            Bukkit.getScheduler().runTask(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Timer")), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }
    }
}
