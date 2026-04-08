package io.github.homeforge.commands;

import io.github.homeforge.HomeForge;
import io.github.homeforge.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final HomeForge plugin;

    public ReloadCommand(HomeForge plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("homeforge.admin.reload")) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("no_permission"));
            return true;
        }
        plugin.reload();
        MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().msg("config_reloaded"));
        return true;
    }
}
