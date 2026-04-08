package io.github.homeforge.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.github.homeforge.HomeForge;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public class BungeeCordListener implements PluginMessageListener {

    private final HomeForge plugin;

    public BungeeCordListener(HomeForge plugin) { this.plugin = plugin; }

    @Override
    public void onPluginMessageReceived(String channel, Player unused, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            if (!in.readUTF().equals("HomeForge")) return;
            in.readShort(); // payload length
            UUID uuid   = UUID.fromString(in.readUTF());
            long homeId = in.readLong();
            plugin.getTeleportManager().registerPendingCrossServer(uuid, homeId);
        } catch (Exception e) {
            plugin.getLogger().warning("[BungeeCord] Failed to process HomeForge message: " + e.getMessage());
        }
    }
}
