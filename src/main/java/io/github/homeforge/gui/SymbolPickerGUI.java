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
 * Icon-picker GUI — lets the player choose a Material as the home's symbol.
 *
 * <p><b>Folia note:</b> The {@code updateSymbol} future completes on the global
 * region thread.  Re-opening the parent settings GUI must be dispatched onto the
 * player's entity region via {@link SchedulerUtil#runOnPlayer}.</p>
 */
public class SymbolPickerGUI implements InventoryHolder {

    private static final Material[] SYMBOLS = {
        Material.GRASS_BLOCK, Material.DIRT, Material.SAND, Material.GRAVEL,
        Material.STONE, Material.COBBLESTONE, Material.OAK_LOG, Material.BIRCH_LOG,
        Material.SPRUCE_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.OAK_LEAVES, Material.CACTUS, Material.LILY_PAD,
        Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
        Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE,
        Material.COAL_BLOCK, Material.IRON_BLOCK, Material.GOLD_BLOCK,
        Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.LAPIS_BLOCK,
        Material.WHEAT, Material.CARROT, Material.POTATO,
        Material.BEETROOT, Material.MELON, Material.PUMPKIN,
        Material.SUGAR_CANE, Material.BAMBOO, Material.COCOA_BEANS,
        Material.BONE, Material.ROTTEN_FLESH, Material.SPIDER_EYE,
        Material.BLAZE_ROD, Material.GHAST_TEAR, Material.SLIME_BALL,
        Material.ENDER_PEARL, Material.PRISMARINE_SHARD, Material.MAGMA_CREAM,
        Material.APPLE, Material.GOLDEN_APPLE, Material.BREAD,
        Material.COOKED_BEEF, Material.GOLDEN_CARROT, Material.PORKCHOP,
        Material.COMPASS, Material.CLOCK, Material.MAP,
        Material.BOOK, Material.NETHER_STAR, Material.BEACON,
        Material.CHEST, Material.CRAFTING_TABLE, Material.FURNACE,
        Material.WHITE_BED, Material.TORCH, Material.LANTERN,
        Material.NETHERRACK, Material.SOUL_SAND, Material.NETHER_BRICK,
        Material.GLOWSTONE, Material.MAGMA_BLOCK, Material.CRIMSON_STEM,
        Material.WARPED_STEM, Material.BLACKSTONE, Material.BASALT,
        Material.END_STONE, Material.PURPUR_BLOCK, Material.DRAGON_EGG,
        Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
        Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
    };

    private static final int BACK_SLOT = 49;

    private final HomeForge       plugin;
    private final UUID            ownerUuid;
    private final Home            home;
    private final HomeSettingsGUI parent;
    private       Inventory       inventory;

    public SymbolPickerGUI(HomeForge plugin, Player viewer, UUID ownerUuid,
                           Home home, HomeSettingsGUI parent) {
        this.plugin    = plugin;
        this.ownerUuid = ownerUuid;
        this.home      = home;
        this.parent    = parent;
    }

    // Build & open  (must be called on the player's entity region)

    public void open(Player viewer) {
        this.inventory = build();
        viewer.openInventory(inventory);
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(this, 54,
                MessageUtil.colorize(
                        plugin.getConfigManager().getGuiSymbolPickerTitle()));

        ItemStack fill = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, fill);

        for (int i = 0; i < SYMBOLS.length && i < 45; i++) {
            inv.setItem(i, makeItem(SYMBOLS[i],
                    "&b" + fmt(SYMBOLS[i].name()),
                    List.of("&7Set as icon for &b" + home.getName() + "&7.")));
        }

        inv.setItem(BACK_SLOT, makeItem(Material.ARROW, "&7← Back",
                List.of("&7Return to home settings.")));
        this.inventory = inv;
        return inv;
    }

    // Click handler  (called from GUIListener, already on the player's region)

    public void handleClick(Player viewer, int slot) {
        if (slot == BACK_SLOT) {
            parent.open(viewer);
            return;
        }
        if (slot < 0 || slot >= SYMBOLS.length) return;

        Material chosen = SYMBOLS[slot];
        home.setSymbol(chosen.name());

        plugin.getHomeManager()
                .updateSymbol(ownerUuid.toString(), home.getName(), chosen.name())
                .thenAccept(ok -> {
                    // thenAccept fires on the global region thread.
                    // Send message here (safe from any thread), then re-open
                    // the parent GUI on the player's entity region.
                    if (ok) MessageUtil.send(viewer,
                            plugin.getConfigManager().getPrefix()
                                    + plugin.getConfigManager().msg("symbol_changed"),
                            "%name%", home.getName());

                    SchedulerUtil.runOnPlayer(plugin, viewer,
                            () -> parent.open(viewer));
                });
    }

    // Helpers

    private static String fmt(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.split("_"))
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1).toLowerCase())
              .append(" ");
        return sb.toString().trim();
    }

    @Override public Inventory getInventory() { return inventory; }
}