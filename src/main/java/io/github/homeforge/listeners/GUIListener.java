package io.github.homeforge.listeners;

import io.github.homeforge.HomeForge;
import io.github.homeforge.gui.HomesGUI;
import io.github.homeforge.gui.HomeSettingsGUI;
import io.github.homeforge.gui.SymbolPickerGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {

    private final HomeForge plugin;

    public GUIListener(HomeForge plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof HomesGUI || h instanceof HomeSettingsGUI || h instanceof SymbolPickerGUI)
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = e.getInventory().getHolder();
        int slot = e.getRawSlot();

        if (holder instanceof HomesGUI gui) {
            e.setCancelled(true);
            if (slot < 0 || slot >= e.getInventory().getSize() || e.getCurrentItem() == null) return;
            gui.handleClick(player, slot);
        } else if (holder instanceof HomeSettingsGUI gui) {
            e.setCancelled(true);
            if (slot < 0 || slot >= e.getInventory().getSize() || e.getCurrentItem() == null) return;
            gui.handleClick(player, slot);
        } else if (holder instanceof SymbolPickerGUI gui) {
            e.setCancelled(true);
            if (slot < 0 || slot >= e.getInventory().getSize() || e.getCurrentItem() == null) return;
            gui.handleClick(player, slot);
        }
    }
}
