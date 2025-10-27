package de.thecoolcraft11.timer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TimerTask extends BukkitRunnable {
    private final TimerManager timerManager;

    public TimerTask(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    @Override
    public void run() {
        
        timerManager.tick();

        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(timerManager.getDisplayText());
        }
    }
}

