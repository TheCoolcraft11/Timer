package de.thecoolcraft11.timer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ResetListener implements Listener {
    private final Timer plugin;

    public ResetListener(Timer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isResetOnPlayerDeath()) {
            return;
        }

        plugin.getLogger().warning("Player death detected, triggering world reset...");
        plugin.scheduleWorldReset(20);
    }
}
