package io.github.homeforge.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.utils.MessageUtil;
import io.github.homeforge.utils.SchedulerUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles home teleportation including cooldowns, movement-checked delays,
 * cross-server BungeeCord forwarding, and sound/particle effects.
 *
 * <p><b>Folia thread model:</b>
 * <ul>
 *   <li>Teleport delay timers use the {@code EntityScheduler} so they tick on the
 *       region owning the player, following them if the region splits/merges.</li>
 *   <li>Post-teleport effects are dispatched back onto the player's entity scheduler
 *       inside the {@code teleportAsync} callback.</li>
 *   <li>Cooldown and pending-teleport maps are {@link ConcurrentHashMap} because
 *       they may be read from multiple region threads simultaneously.</li>
 * </ul>
 * </p>
 */
public class TeleportManager {

    private final HomeForge plugin;

    // ScheduledTask replaces BukkitTask — works on both Paper and Folia.
    private final Map<UUID, ScheduledTask> pendingTeleports   = new ConcurrentHashMap<>();
    private final Map<UUID, Location>      teleportOrigins    = new ConcurrentHashMap<>();
    private final Map<UUID, Long>          cooldowns          = new ConcurrentHashMap<>();
    private final Map<UUID, Long>          pendingCrossServer = new ConcurrentHashMap<>();

    public TeleportManager(HomeForge plugin) {
        this.plugin = plugin;
    }

    // Public API

    /**
     * Initiate a teleport to {@code home} for {@code player}.
     * Must be called from the player's entity region (event / command context).
     */
    public void teleportToHome(Player player, Home home) {

        // --- Cooldown check ---
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

        // --- Already in a delayed teleport ---
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("delayed_teleport_already_in_progress"));
            return;
        }

        // --- Cross-server redirect ---
        String curServer  = plugin.getConfigManager().getServerName();
        String homeServer = home.getServerName();
        if (homeServer != null && !homeServer.isBlank()
                && !homeServer.equalsIgnoreCase(curServer)) {
            sendToServer(player, home);
            return;
        }

        // --- Resolve destination ---
        Location dest = home.toBukkitLocation();
        if (dest == null) {
            player.sendMessage(MessageUtil.colorize(plugin.getConfigManager().getPrefix()
                    + "&cWorld &b" + home.getWorld() + "&c is not loaded."));
            return;
        }

        int delay = plugin.getConfigManager().getTeleportDelay();
        if (delay <= 0) {
            doTeleport(player, dest, home.getName());
        } else {
            startDelayedTeleport(player, dest, home.getName(), delay);
        }
    }

    // Delayed teleport — entity scheduler keeps it pinned to the player

    private void startDelayedTeleport(Player player, Location dest,
                                      String name, int delaySeconds) {
        UUID uuid = player.getUniqueId();
        teleportOrigins.put(uuid, player.getLocation().clone());

        MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().msg("delayed_teleport"),
                "%seconds%", String.valueOf(delaySeconds));

        // Mutable countdown wrapped in an array so the lambda can capture it.
        int[] countdown = {delaySeconds};

        /*
         * runAtFixedRate on the EntityScheduler — fires every 20 ticks (1 s)
         * on whatever region currently owns the player.
         * initialDelayTicks = 20 so the first decrement happens 1 s after scheduling.
         */
        ScheduledTask task = SchedulerUtil.runTimerOnPlayer(plugin, player, scheduledTask -> {
            countdown[0]--;

            // Movement cancellation check
            if (plugin.getConfigManager().checkMovementOnDelay()) {
                Location origin = teleportOrigins.get(uuid);
                if (origin != null
                        && origin.distanceSquared(player.getLocation()) > 0.0225) {
                    MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().msg("delayed_teleport_cancel_movement"));
                    scheduledTask.cancel();
                    pendingTeleports.remove(uuid);
                    teleportOrigins.remove(uuid);
                    return;
                }
            }

            // Countdown finished — teleport!
            if (countdown[0] <= 0) {
                scheduledTask.cancel();
                pendingTeleports.remove(uuid);
                teleportOrigins.remove(uuid);
                doTeleport(player, dest, name);
            }
        }, 20L, 20L);

        pendingTeleports.put(uuid, task);
    }

    // Actual teleport + post-teleport effects

    /**
     * Execute the teleport.  {@code player.teleportAsync} is async internally;
     * effects are dispatched back onto the player's entity scheduler inside the
     * completion callback so they run on the correct region thread.
     */
    private void doTeleport(Player player, Location dest, String name) {
        MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().msg("home_teleport"), "%name%", name);

        player.teleportAsync(dest).thenAccept(success -> {
            if (!success) return;
            // Post-teleport effects must run on the player's region thread.
            SchedulerUtil.runOnPlayer(plugin, player, () -> {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

                if (plugin.getConfigManager().playSoundOnTeleport())
                    player.playSound(player.getLocation(),
                            Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                if (plugin.getConfigManager().playParticleOnTeleport())
                    player.getWorld().spawnParticle(Particle.PORTAL,
                            player.getLocation().add(0, 1, 0),
                            30, 0.3, 0.5, 0.3, 0.1);
            });
        });
    }

    // Cross-server (BungeeCord)

    private void sendToServer(Player player, Home home) {
        if (!plugin.getConfigManager().isBungeeCordEnabled()) return;

        MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().msg("home_cross_server"),
                "%server%", home.getServerName(), "%name%", home.getName());

        try {
            // Forward message carrying the home ID to the target server.
            ByteArrayDataOutput fwd = ByteStreams.newDataOutput();
            fwd.writeUTF("Forward");
            fwd.writeUTF(home.getServerName());
            fwd.writeUTF("HomeForge");

            ByteArrayDataOutput payload = ByteStreams.newDataOutput();
            payload.writeUTF(player.getUniqueId().toString());
            payload.writeLong(home.getId());
            byte[] pb = payload.toByteArray();
            fwd.writeShort(pb.length);
            fwd.write(pb);

            player.sendPluginMessage(plugin, "BungeeCord", fwd.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "Failed to send BungeeCord Forward: " + e.getMessage());
        }

        // Then connect the player.
        ByteArrayDataOutput connect = ByteStreams.newDataOutput();
        connect.writeUTF("Connect");
        connect.writeUTF(home.getServerName());
        player.sendPluginMessage(plugin, "BungeeCord", connect.toByteArray());
    }

    // Lifecycle helpers

    /**
     * Cancel any pending delayed teleport for {@code player} (e.g. on disconnect).
     */
    public void cancelTeleport(Player player) {
        ScheduledTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) task.cancel();
        teleportOrigins.remove(player.getUniqueId());
    }

    /**
     * Cancel ALL pending teleports — called on plugin disable.
     */
    public void cancelAllTeleports() {
        pendingTeleports.values().forEach(ScheduledTask::cancel);
        pendingTeleports.clear();
        teleportOrigins.clear();
    }

    // Accessors

    public boolean  hasPendingTeleport(UUID uuid)      { return pendingTeleports.containsKey(uuid); }
    public Location getTeleportOrigin(UUID uuid)        { return teleportOrigins.get(uuid); }
    public void     clearCooldown(UUID uuid)            { cooldowns.remove(uuid); }

    public void registerPendingCrossServer(UUID uuid, long homeId) {
        pendingCrossServer.put(uuid, homeId);
    }

    public Long consumePendingCrossServer(UUID uuid) {
        return pendingCrossServer.remove(uuid);
    }
}