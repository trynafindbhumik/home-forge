package io.github.homeforge.gui;

import io.github.homeforge.HomeForge;
import io.github.homeforge.models.Home;
import io.github.homeforge.models.PlayerData;
import io.github.homeforge.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomesGUI implements InventoryHolder {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final HomeForge  plugin;
    private final UUID       viewerUuid;
    private final UUID       ownerUuid;
    private final String     ownerName;
    private final List<Home> homes;
    private       int        page;
    private       Inventory  inventory;

    public HomesGUI(HomeForge plugin, Player viewer, UUID ownerUuid,
                    String ownerName, List<Home> homes, int page) {
        this.plugin     = plugin;
        this.viewerUuid = viewer.getUniqueId();
        this.ownerUuid  = ownerUuid;
        this.ownerName  = ownerName;
        this.homes      = homes;
        this.page       = page;
    }

    public void open(Player viewer) {
        this.inventory = build();
        viewer.openInventory(inventory);
    }

    private Inventory build() {
        boolean isSelf  = viewerUuid.equals(ownerUuid);
        String  rawTitle = isSelf
                ? plugin.getConfigManager().getGuiTitle()
                : MessageUtil.replace(plugin.getConfigManager().getGuiAdminTitle(), "%player%", ownerName);

        Inventory inv = Bukkit.createInventory(this, 54, MessageUtil.colorize(rawTitle));

        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = PAGE_SIZE; i < 54; i++) inv.setItem(i, filler);

        int start = page * PAGE_SIZE;
        for (int i = start; i < Math.min(start + PAGE_SIZE, homes.size()); i++)
            inv.setItem(i - start, homeItem(homes.get(i)));

        if (page > 0)
            inv.setItem(PREV_SLOT, makeItem(Material.ARROW, "&7◀ &aPrevious page"));

        PlayerData data  = plugin.getHomeManager().getPlayerData(ownerUuid.toString());
        Player ownerOnline = Bukkit.getPlayer(ownerUuid);
        int limit = ownerOnline != null
                ? plugin.getHomeManager().getHomeLimit(ownerOnline)
                : plugin.getConfigManager().getDefaultHomeLimit() + (int) data.getExtraHomes();

        inv.setItem(INFO_SLOT, makeItem(Material.BOOK,
                "&bHomes &7— &f" + homes.size() + "&7/&f" + limit,
                List.of("&7Page " + (page + 1), "&7Bonus slots: &b" + data.getExtraHomes())));

        if ((page + 1) * PAGE_SIZE < homes.size())
            inv.setItem(NEXT_SLOT, makeItem(Material.ARROW, "&aNext page &7▶"));

        this.inventory = inv;
        return inv;
    }

    private ItemStack homeItem(Home home) {
        PlayerData pd   = plugin.getHomeManager().getPlayerData(ownerUuid.toString());
        boolean primary = pd.getPrimaryHome() != null && pd.getPrimaryHome() == home.getId();

        List<String> lore = new ArrayList<>();
        lore.add("&7World: &f" + home.getWorld());
        lore.add("&7X: &f" + String.format("%.1f", home.getLocX())
                + " &7Y: &f" + String.format("%.1f", home.getLocY())
                + " &7Z: &f" + String.format("%.1f", home.getLocZ()));
        lore.add("&7Last used: &f" + DATE_FMT.format(Instant.ofEpochMilli(home.getLastUsed())));
        if (home.getServerName() != null && !home.getServerName().isBlank())
            lore.add("&7Server: &b" + home.getServerName());
        lore.add("");
        lore.add("&eClick &7to open settings");

        return makeItem(home.getSymbolMaterial(),
                (primary ? "&e★ " : "&b") + home.getName(), lore);
    }

    public void handleClick(Player viewer, int slot) {
        if (slot == PREV_SLOT && page > 0) {
            page--; build(); viewer.openInventory(inventory); return;
        }
        if (slot == NEXT_SLOT && (page + 1) * PAGE_SIZE < homes.size()) {
            page++; build(); viewer.openInventory(inventory); return;
        }
        int idx = page * PAGE_SIZE + slot;
        if (idx < 0 || idx >= homes.size()) return;
        new HomeSettingsGUI(plugin, viewer, ownerUuid, ownerName, homes.get(idx), this).open(viewer);
    }

    @Override public Inventory  getInventory()  { return inventory; }
    public        UUID          getViewerUuid() { return viewerUuid; }
    public        UUID          getOwnerUuid()  { return ownerUuid; }
    public        List<Home>    getHomes()      { return homes; }

    public static ItemStack makeItem(Material mat, String name) {
        return makeItem(mat, name, List.of());
    }

    public static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(MessageUtil.colorize(name));
        List<Component> lc = new ArrayList<>();
        for (String l : lore) lc.add(MessageUtil.colorize(l));
        meta.lore(lc);
        item.setItemMeta(meta);
        return item;
    }
}
