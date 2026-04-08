package io.github.homeforge.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private final HomeForge plugin;
    private final Map<UUID, BukkitTask> pendingTeleports  = new ConcurrentHashMap<>();
    private final Map<UUID, Location>   teleportOrigins   = new ConcurrentHashMap<>();
    private final Map<UUID, Long>       cooldowns         = new ConcurrentHashMap<>();
    private final Map<UUID, Long>       pendingCrossServer = new ConcurrentHashMap<>();

    public TeleportManager(HomeForge plugin) {
        this.plugin = plugin;
    }

    public void teleportToHome(Player player, Home home) {
        // Cooldown
        int cooldownSec = plugin.getConfigManager().getHomeCooldown();
        if (cooldownSec > 0 && !player.hasPermission("homeforge.cooldown.bypass")) {
            Long last = cooldowns.get(player.getUniqueId());
            if (last != null) {
                long remaining = cooldownSec - (System.currentTimeMillis() - last) / 1000;
                if (remaining > 0) {
                    MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().msg("cooldown"),
                            "%seconds%", String.valueOf(remaining));
                    return;
                }
            }
        }

        // Already teleporting
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("delayed_teleport_already_in_progress"));
            return;
        }

        // Cross-server
        String curServer  = plugin.getConfigManager().getServerName();
        String homeServer = home.getServerName();
        if (homeServer != null && !homeServer.isBlank() && !homeServer.equalsIgnoreCase(curServer)) {
            sendToServer(player, home);
            return;
        }

        Location dest = home.toBukkitLocation();
        if (dest == null) {
            player.sendMessage(MessageUtil.colorize(plugin.getConfigManager().getPrefix()
                    + "&cWorld &b" + home.getWorld() + "&c is not loaded."));
            return;
        }

        int delay = plugin.getConfigManager().getTeleportDelay();
        if (delay <= 0) doTeleport(player, dest, home.getName());
        else            startDelayedTeleport(player, dest, home.getName(), delay);
    }

    private void startDelayedTeleport(Player player, Location dest, String name, int delaySeconds) {
        teleportOrigins.put(player.getUniqueId(), player.getLocation().clone());
        MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().msg("delayed_teleport"),
                "%seconds%", String.valueOf(delaySeconds));

        int[] countdown = {delaySeconds};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            countdown[0]--;
            if (plugin.getConfigManager().checkMovementOnDelay()) {
                Location origin = teleportOrigins.get(player.getUniqueId());
                if (origin != null && origin.distanceSquared(player.getLocation()) > 0.0225) {
                    MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().msg("delayed_teleport_cancel_movement"));
                    cancelTeleport(player);
                    return;
                }
            }
            if (countdown[0] <= 0) {
                pendingTeleports.remove(player.getUniqueId());
                teleportOrigins.remove(player.getUniqueId());
                doTeleport(player, dest, name);
            }
        }, 20L, 20L);
        pendingTeleports.put(player.getUniqueId(), task);
    }

    private void doTeleport(Player player, Location dest, String name) {
        MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().msg("home_teleport"), "%name%", name);
        player.teleportAsync(dest).thenAccept(success -> {
            if (!success) return;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                if (plugin.getConfigManager().playSoundOnTeleport())
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                if (plugin.getConfigManager().playParticleOnTeleport())
                    player.getWorld().spawnParticle(Particle.PORTAL,
                            player.getLocation().add(0,1,0), 30, 0.3, 0.5, 0.3, 0.1);
            });
        });
    }

    private void sendToServer(Player player, Home home) {
        if (!plugin.getConfigManager().isBungeeCordEnabled()) return;
        MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().msg("home_cross_server"),
                "%server%", home.getServerName(), "%name%", home.getName());
        try {
            ByteArrayDataOutput fwd = ByteStreams.newDataOutput();
            fwd.writeUTF("Forward"); fwd.writeUTF(home.getServerName());
            fwd.writeUTF("HomeForge");
            ByteArrayDataOutput payload = ByteStreams.newDataOutput();
            payload.writeUTF(player.getUniqueId().toString());
            payload.writeLong(home.getId());
            byte[] pb = payload.toByteArray();
            fwd.writeShort(pb.length); fwd.write(pb);
            player.sendPluginMessage(plugin, "BungeeCord", fwd.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send BungeeCord Forward: " + e.getMessage());
        }
        ByteArrayDataOutput connect = ByteStreams.newDataOutput();
        connect.writeUTF("Connect"); connect.writeUTF(home.getServerName());
        player.sendPluginMessage(plugin, "BungeeCord", connect.toByteArray());
    }

    public void cancelTeleport(Player player) {
        BukkitTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) task.cancel();
        teleportOrigins.remove(player.getUniqueId());
    }

    public boolean hasPendingTeleport(UUID uuid)    { return pendingTeleports.containsKey(uuid); }
    public Location getTeleportOrigin(UUID uuid)    { return teleportOrigins.get(uuid); }
    public void clearCooldown(UUID uuid)            { cooldowns.remove(uuid); }
    public void cancelAllTeleports()                { pendingTeleports.values().forEach(BukkitTask::cancel); pendingTeleports.clear(); teleportOrigins.clear(); }
    public void registerPendingCrossServer(UUID u, long homeId) { pendingCrossServer.put(u, homeId); }
    public Long consumePendingCrossServer(UUID u)   { return pendingCrossServer.remove(u); }
}
