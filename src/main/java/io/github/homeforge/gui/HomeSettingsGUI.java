package io.github.homeforge.gui;

import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.utils.MessageUtil;
import io.github.homeforge.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

import static io.github.homeforge.gui.HomesGUI.makeItem;

/**
 * Per-home management GUI: teleport, relocate, set primary, change symbol, delete.
 *
 * <p><b>Folia note:</b> clicks arrive on the player's entity region, so all
 * direct GUI opens ({@link #open}) are safe without an extra dispatch.
 * However, {@code CompletableFuture#thenAccept} callbacks produced by
 * {@link io.github.homeforge.managers.HomeManager} complete on the global
 * region thread — any code in those callbacks that touches the player (opens
 * inventories, etc.) must be re-dispatched via {@link SchedulerUtil#runOnPlayer}.</p>
 */
public class HomeSettingsGUI implements InventoryHolder {

    private static final int SLOT_TELEPORT      = 11;
    private static final int SLOT_UPDATE_LOC    = 12;
    private static final int SLOT_SET_PRIMARY   = 13;
    private static final int SLOT_CHANGE_SYMBOL = 14;
    private static final int SLOT_DELETE        = 15;
    private static final int SLOT_BACK          = 22;

    private final HomeForge plugin;
    private final UUID      ownerUuid;
    private final String    ownerName;
    private final Home      home;
    private final HomesGUI  parentGUI;
    private       Inventory inventory;

    public HomeSettingsGUI(HomeForge plugin, Player viewer, UUID ownerUuid,
                           String ownerName, Home home, HomesGUI parentGUI) {
        this.plugin    = plugin;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.home      = home;
        this.parentGUI = parentGUI;
    }

    // Build & open  (must be called on the player's entity region)

    public void open(Player viewer) {
        this.inventory = build(viewer);
        viewer.openInventory(inventory);
    }

    private Inventory build(Player viewer) {
        String title = MessageUtil.replace(
                plugin.getConfigManager().getGuiSettingsTitle(),
                "%name%", home.getName());

        Inventory inv = Bukkit.createInventory(this, 27, MessageUtil.colorize(title));
        ItemStack fill = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, fill);

        boolean isOwner = viewer.getUniqueId().equals(ownerUuid);
        boolean isAdmin = viewer.hasPermission("homeforge.admin.editother");

        inv.setItem(SLOT_TELEPORT, makeItem(Material.ENDER_PEARL,
                "&aTeleport to &b" + home.getName(),
                List.of("&7Teleports you to this home.")));

        if (isOwner || isAdmin)
            inv.setItem(SLOT_UPDATE_LOC, makeItem(Material.COMPASS,
                    "&eUpdate Location",
                    List.of("&7Move home to your current position.")));

        if (isOwner) {
            boolean isPrimary = isPrimary();
            inv.setItem(SLOT_SET_PRIMARY, makeItem(
                    isPrimary ? Material.NETHER_STAR : Material.IRON_NUGGET,
                    isPrimary ? "&e★ Already Primary" : "&6Set as Primary",
                    List.of("&7Default destination for &b/home&7.")));
        }

        if (isOwner || isAdmin)
            inv.setItem(SLOT_CHANGE_SYMBOL, makeItem(home.getSymbolMaterial(),
                    "&bChange Symbol",
                    List.of("&7Pick a new icon for this home.")));

        if (isOwner || isAdmin)
            inv.setItem(SLOT_DELETE, makeItem(Material.BARRIER,
                    "&cDelete Home",
                    List.of("&7Permanently remove &b" + home.getName() + "&7.",
                            "", "&cCannot be undone!")));

        inv.setItem(SLOT_BACK, makeItem(Material.ARROW, "&7← Back",
                List.of("&7Return to home list.")));

        this.inventory = inv;
        return inv;
    }

    // Click handler  (called from GUIListener, already on the player's region)

    public void handleClick(Player viewer, int slot) {
        boolean isOwner = viewer.getUniqueId().equals(ownerUuid);
        boolean isAdmin = viewer.hasPermission("homeforge.admin.editother");

        switch (slot) {

            // Teleport
            case SLOT_TELEPORT -> {
                viewer.closeInventory();
                plugin.getTeleportManager().teleportToHome(viewer, home);
            }

            // Update location
            case SLOT_UPDATE_LOC -> {
                if (!isOwner && !isAdmin) return;
                viewer.closeInventory();
                plugin.getHomeManager()
                        .updateHomeLocation(viewer, home.getName())
                        .thenAccept(ok -> {
                            // thenAccept fires on global region — message only, safe.
                            if (ok) MessageUtil.send(viewer,
                                    plugin.getConfigManager().getPrefix()
                                            + plugin.getConfigManager()
                                                    .msg("home_location_updated"),
                                    "%name%", home.getName());
                        });
            }

            // Set primary
            case SLOT_SET_PRIMARY -> {
                if (!isOwner || isPrimary()) return;
                plugin.getHomeManager()
                        .setPrimaryHome(ownerUuid.toString(), home.getName())
                        .thenAccept(ok -> {
                            if (!ok) return;
                            // Message is safe on global region.
                            MessageUtil.send(viewer,
                                    plugin.getConfigManager().getPrefix()
                                            + plugin.getConfigManager()
                                                    .msg("primary_home_set"),
                                    "%name%", home.getName());
                            // Re-opening the inventory must happen on the
                            // player's entity region, not the global region.
                            SchedulerUtil.runOnPlayer(plugin, viewer,
                                    () -> open(viewer));
                        });
            }

            // Change symbol
            case SLOT_CHANGE_SYMBOL -> {
                if (!isOwner && !isAdmin) return;
                new SymbolPickerGUI(plugin, viewer, ownerUuid, home, this)
                        .open(viewer);
            }

            // Delete
            case SLOT_DELETE -> {
                if (!isOwner && !isAdmin) return;
                viewer.closeInventory();
                plugin.getHomeManager()
                        .removeHome(ownerUuid.toString(), home.getName())
                        .thenAccept(ok -> {
                            // Message only — safe on global region.
                            if (ok) MessageUtil.send(viewer,
                                    plugin.getConfigManager().getPrefix()
                                            + plugin.getConfigManager()
                                                    .msg("home_removed"),
                                    "%name%", home.getName());
                        });
            }

            // Back
            case SLOT_BACK -> parentGUI.open(viewer);
        }
    }

    // Helpers

    private boolean isPrimary() {
        Long ph = plugin.getHomeManager()
                .getPlayerData(ownerUuid.toString()).getPrimaryHome();
        return ph != null && ph == home.getId();
    }

    @Override public Inventory getInventory() { return inventory; }
    public Home     getHome()      { return home; }
    public UUID     getOwnerUuid() { return ownerUuid; }
    public HomesGUI getParentGUI() { return parentGUI; }
}