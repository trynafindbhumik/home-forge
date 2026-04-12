package io.github.homeforge.commands;

import io.github.homeforge.HomeForge;
import io.github.homeforge.gui.HomesGUI;
import io.github.homeforge.models.Home;
import io.github.homeforge.models.PlayerData;
import io.github.homeforge.utils.MessageUtil;
import io.github.homeforge.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ADMIN_SUBS = List.of("add", "remove", "set", "info");
    private final HomeForge plugin;

    public HomesCommand(HomeForge plugin) { this.plugin = plugin; }

    // Command dispatch

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {

        // /homes — open own GUI
        if (args.length == 0) {
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
            openGUI(player, player.getUniqueId(), player.getName());
            return true;
        }

        String sub = args[0].toLowerCase();

        // Admin sub-commands (add / remove / set / info)
        if (ADMIN_SUBS.contains(sub)) {
            return handleAdmin(sender, sub, args);
        }

        // /homes <player> — admin view of another player's homes
        if (!(sender instanceof Player viewer)) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("players_only"));
            return true;
        }
        if (!viewer.hasPermission("homeforge.admin.viewother")) {
            MessageUtil.send(viewer, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("no_permission"));
            return true;
        }
        resolvePlayer(args[0], sender,
                (uuid, name) -> openGUI(viewer, uuid, name));
        return true;
    }

    // Admin sub-command handler

    private boolean handleAdmin(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission("homeforge.admin.extrahomes")) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("no_permission"));
            return true;
        }

        // /homes info <player>
        if (sub.equals("info")) {
            if (args.length < 2) {
                MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                        + "&cUsage: /homes info <player>");
                return true;
            }
            resolvePlayer(args[1], sender, (uuid, name) -> {
                List<Home>  homes = plugin.getHomeManager().getHomes(uuid.toString());
                PlayerData  data  = plugin.getHomeManager().getPlayerData(uuid.toString());
                Player      online = Bukkit.getPlayer(uuid);
                int limit = online != null
                        ? plugin.getHomeManager().getHomeLimit(online)
                        : plugin.getConfigManager().getDefaultHomeLimit()
                                + (int) data.getExtraHomes();
                MessageUtil.send(sender,
                        plugin.getConfigManager().getPrefix()
                                + plugin.getConfigManager().msg("homes_info"),
                        "%player%", name,
                        "%homes%",  String.valueOf(homes.size()),
                        "%max%",    String.valueOf(limit),
                        "%extra%",  String.valueOf(data.getExtraHomes()));
            });
            return true;
        }

        // /homes add|remove|set <player> <amount>
        if (args.length < 3) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + "&cUsage: /homes " + sub + " <player> <amount>");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().msg("invalid_amount"));
            return true;
        }

        final long finalAmount = amount;
        resolvePlayer(args[1], sender, (uuid, name) -> {
            switch (sub) {
                case "add" -> plugin.getHomeManager()
                        .addExtraHomes(uuid.toString(), finalAmount)
                        .thenAccept(d -> MessageUtil.send(sender,
                                plugin.getConfigManager().getPrefix()
                                        + plugin.getConfigManager().msg("extra_homes_added"),
                                "%player%", name,
                                "%amount%", String.valueOf(finalAmount),
                                "%total%",  String.valueOf(d.getExtraHomes())));

                case "remove" -> plugin.getHomeManager()
                        .removeExtraHomes(uuid.toString(), finalAmount)
                        .thenAccept(d -> MessageUtil.send(sender,
                                plugin.getConfigManager().getPrefix()
                                        + plugin.getConfigManager().msg("extra_homes_removed"),
                                "%player%", name,
                                "%amount%", String.valueOf(finalAmount),
                                "%total%",  String.valueOf(d.getExtraHomes())));

                case "set" -> plugin.getHomeManager()
                        .setExtraHomes(uuid.toString(), finalAmount)
                        .thenAccept(d -> MessageUtil.send(sender,
                                plugin.getConfigManager().getPrefix()
                                        + plugin.getConfigManager().msg("extra_homes_set"),
                                "%player%", name,
                                "%amount%", String.valueOf(finalAmount)));
            }
        });
        return true;
    }

    // GUI helpers

    /**
     * Load {@code ownerUuid}'s homes asynchronously, then open the GUI on
     * {@code viewer}'s entity region thread (required for inventory opens on Folia).
     */
    private void openGUI(Player viewer, UUID ownerUuid, String ownerName) {
        plugin.getHomeManager().loadHomesAsync(ownerUuid.toString())
                .thenAccept(homes -> {
                    // loadHomesAsync completes on the global region thread.
                    // Opening an inventory requires the player's entity region.
                    SchedulerUtil.runOnPlayer(plugin, viewer, () -> {
                        if (homes.isEmpty()) {
                            MessageUtil.send(viewer,
                                    plugin.getConfigManager().getPrefix()
                                            + plugin.getConfigManager().msg(
                                                    viewer.getUniqueId().equals(ownerUuid)
                                                            ? "no_homes"
                                                            : "no_home_other"));
                            return;
                        }
                        new HomesGUI(plugin, viewer, ownerUuid, ownerName, homes, 0)
                                .open(viewer);
                    });
                });
    }

    // Player resolution (online → offline lookup is async)

    /**
     * Resolve a player name to a UUID + display name, then invoke {@code cb}.
     *
     * <p>If the player is online the callback fires synchronously on the calling
     * thread.  For offline players the lookup is dispatched to an async thread
     * and the callback fires on the global region thread.</p>
     */
    @SuppressWarnings("deprecation")
    private void resolvePlayer(String name, CommandSender errorSink,
                               BiConsumer<UUID, String> cb) {
        // Online player — fast path, no thread switch needed.
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            cb.accept(online.getUniqueId(), online.getName());
            return;
        }

        // Offline player — Bukkit.getOfflinePlayer blocks on name→UUID look-up.
        SchedulerUtil.runAsync(plugin, () -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
            if (!offline.hasPlayedBefore()) {
                SchedulerUtil.runGlobal(plugin, () ->
                        MessageUtil.send(errorSink,
                                plugin.getConfigManager().getPrefix()
                                        + plugin.getConfigManager()
                                                .msg("player_not_found"),
                                "%player%", name));
                return;
            }
            String resolvedName = offline.getName() != null
                    ? offline.getName() : name;
            // Deliver callback on global region; openGUI then uses entity
            // scheduler internally for the inventory open.
            SchedulerUtil.runGlobal(plugin, () ->
                    cb.accept(offline.getUniqueId(), resolvedName));
        });
    }

    // Tab completion

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            return Stream.concat(
                    ADMIN_SUBS.stream(),
                    Bukkit.getOnlinePlayers().stream().map(Player::getName))
                    .filter(s -> s.toLowerCase().startsWith(typed))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && ADMIN_SUBS.contains(args[0].toLowerCase())) {
            String typed = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(typed))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}