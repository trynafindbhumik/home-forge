package io.github.homeforge.listeners;

import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.utils.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final HomeForge plugin;

    public PlayerListener(HomeForge plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        // Load homes/player-data from DB (async) into cache (global region).
        plugin.getHomeManager().loadPlayer(player);

        // Cross-server teleport: if this player arrived via BungeeCord with a
        // pending home ID, teleport them a few ticks after login so the world
        // has fully loaded.  Use the entity scheduler so the task tracks the
        // player's region, which may differ from the spawn region.
        Long pendingHomeId = plugin.getTeleportManager()
                .consumePendingCrossServer(player.getUniqueId());

        if (pendingHomeId != null) {
            final long homeId = pendingHomeId;
            SchedulerUtil.runOnPlayerLater(plugin, player, () -> {
                Home home = plugin.getHomeManager()
                        .getDatabase().getHomeById(homeId);
                if (home != null) {
                    plugin.getTeleportManager().teleportToHome(player, home);
                }
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        plugin.getTeleportManager().cancelTeleport(player);
        plugin.getTeleportManager().clearCooldown(player.getUniqueId());
        plugin.getHomeManager().unloadPlayer(player);
    }
}