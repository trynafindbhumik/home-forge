package io.github.homeforge.commands;

import io.github.homeforge.HomeForge;
import io.github.homeforge.managers.HomeManager;
import io.github.homeforge.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SetHomeCommand implements CommandExecutor, TabCompleter {

    private static final String DEFAULT_NAME = "home";
    private final HomeForge plugin;

    public SetHomeCommand(HomeForge plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("players_only"));
            return true;
        }
        if (plugin.getConfigManager().usePermissions()
                && !player.hasPermission("homeforge.use")) {
            MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("no_permission"));
            return true;
        }

        String name = args.length >= 1 ? args[0] : DEFAULT_NAME;

        if (!HomeManager.NAME_PATTERN.matcher(name).matches()) {
            MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("home_name_invalid"));
            return true;
        }

        String uuid    = player.getUniqueId().toString();
        boolean exists = plugin.getHomeManager().getHome(uuid, name) != null;

        if (!exists) {
            int current = plugin.getHomeManager().getHomes(uuid).size();
            int limit   = plugin.getHomeManager().getHomeLimit(player);
            if (current >= limit) {
                MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                                + plugin.getConfigManager().msg("home_limit_reached"),
                        "%current%", String.valueOf(current),
                        "%max%",     String.valueOf(limit));
                return true;
            }
        }

        plugin.getHomeManager().setHome(player, name, true).thenAccept(ok -> {
            if (ok) MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                            + plugin.getConfigManager().msg(exists ? "home_location_updated" : "home_set"),
                    "%name%", name);
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
