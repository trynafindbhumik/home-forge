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

public class RemoveHomeCommand implements CommandExecutor, TabCompleter {

    private final HomeForge plugin;

    public RemoveHomeCommand(HomeForge plugin) { this.plugin = plugin; }

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
        if (args.length == 0) {
            MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + "&cUsage: /" + label + " <name>");
            return true;
        }

        String name = args[0];
        String uuid = player.getUniqueId().toString();

        if (plugin.getHomeManager().getHome(uuid, name) == null) {
            MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("home_not_found"), "%name%", name);
            return true;
        }

        plugin.getHomeManager().removeHome(uuid, name).thenAccept(ok -> {
            if (ok) MessageUtil.send(player, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("home_removed"), "%name%", name);
        });
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
