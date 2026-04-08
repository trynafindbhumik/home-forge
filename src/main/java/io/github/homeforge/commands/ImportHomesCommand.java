package io.github.homeforge.commands;

import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

// /importhomes essentials
// Reads EssentialsX player YAML files and imports all homes into HomeForge.
public class ImportHomesCommand implements CommandExecutor {

    private final HomeForge plugin;

    public ImportHomesCommand(HomeForge plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("homeforge.admin.import")) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("no_permission"));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("essentials")) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("import_usage"));
            return true;
        }

        File essentialsDir = new File(plugin.getDataFolder().getParentFile(), "Essentials/userdata");
        if (!essentialsDir.exists() || !essentialsDir.isDirectory()) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + MessageUtil.replace(plugin.getConfigManager().msg("import_failed"),
                            "%error%", "EssentialsX userdata folder not found"));
            return true;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            AtomicInteger count = new AtomicInteger(0);
            String serverName  = plugin.getConfigManager().getServerName();

            File[] files = essentialsDir.listFiles(f -> f.getName().endsWith(".yml"));
            if (files == null || files.length == 0) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                                + plugin.getConfigManager().msg("import_nothing")));
                return;
            }

            for (File file : files) {
                String uuidStr = file.getName().replace(".yml", "");
                try { java.util.UUID.fromString(uuidStr); }
                catch (IllegalArgumentException ignored) { continue; }

                YamlConfiguration yaml;
                try { yaml = YamlConfiguration.loadConfiguration(file); }
                catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Skipping " + file.getName(), e);
                    continue;
                }

                ConfigurationSection homesSection = yaml.getConfigurationSection("homes");
                if (homesSection == null) continue;

                for (String homeName : homesSection.getKeys(false)) {
                    if (plugin.getHomeManager().getHome(uuidStr, homeName) != null) continue;
                    ConfigurationSection hs = homesSection.getConfigurationSection(homeName);
                    if (hs == null) continue;
                    try {
                        Home h = new Home();
                        h.setId(plugin.getDatabase().nextHomeId());
                        h.setOwner(uuidStr);
                        h.setServerName(serverName.isBlank() ? null : serverName);
                        h.setWorld(hs.getString("world", "world"));
                        h.setLocX(hs.getDouble("x", 0));
                        h.setLocY(hs.getDouble("y", 64));
                        h.setLocZ(hs.getDouble("z", 0));
                        h.setLocYaw((float) hs.getDouble("yaw", 0));
                        h.setLocPitch((float) hs.getDouble("pitch", 0));
                        h.setName(homeName);
                        h.setSymbol("GRASS_BLOCK");
                        h.setLastUsed(System.currentTimeMillis());
                        if (plugin.getDatabase().insertHome(h)) count.incrementAndGet();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to import home '" + homeName + "' for " + uuidStr, e);
                    }
                }
            }

            int total = count.get();
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().msg(total == 0 ? "import_nothing" : "import_success"),
                            "%count%", String.valueOf(total)));
        });
        return true;
    }
}
