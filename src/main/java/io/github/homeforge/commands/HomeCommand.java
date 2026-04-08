package io.github.homeforge.commands;

import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final HomeForge plugin;

    public HomeCommand(HomeForge plugin) { this.plugin = plugin; }

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

        String uuid = player.getUniqueId().toString();
        List<Home> homes = plugin.getHomeManager().getHomes(uuid);

        if (homes.isEmpty()) {
            MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("no_homes"));
            return true;
        }

        Home target;
        if (args.length == 0) {
            target = plugin.getHomeManager().getPrimaryHome(uuid);
            if (target == null) {
                target = homes.stream()
                        .min((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                        .orElse(null);
            }
            if (target == null) {
                MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().msg("no_primary_home"));
                return true;
            }
        } else {
            target = plugin.getHomeManager().getHome(uuid, args[0]);
            if (target == null) {
                MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                                + plugin.getConfigManager().msg("home_not_found"),
                        "%name%", args[0]);
                return true;
            }
        }

        plugin.getTeleportManager().teleportToHome(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return List.of();
        String typed = args[0].toLowerCase();
        return plugin.getHomeManager().getHomes(player.getUniqueId().toString())
                .stream().map(Home::getName)
                .filter(n -> n.toLowerCase().startsWith(typed))
                .collect(Collectors.toList());
    }
}
