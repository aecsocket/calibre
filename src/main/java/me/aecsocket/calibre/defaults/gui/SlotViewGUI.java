package me.aecsocket.calibre.defaults.gui;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.gui.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SlotViewGUI extends GUI {
    public static final InventorySize INVENTORY_SIZE = new InventorySize(6);

    private final CalibrePlugin plugin;
    private final GUIManager guiManager;
    private CalibreComponent component;

    public SlotViewGUI(CalibrePlugin plugin, GUIManager guiManager, CalibreComponent component) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.component = component;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    public CalibreComponent getComponent() { return component; }
    public void setComponent(CalibreComponent component) { this.component = component; }

    @Override public GUIManager getGUIManager() { return guiManager; }
    @Override public InventorySize getSize(Player player) { return INVENTORY_SIZE; }
    @Override public String getTitle(Player player) { return plugin.gen(player, "gui.slot_view"); }

    @Override
    public Map<Integer, GUIItem> getItems(Player player) {
        Map<Integer, GUIItem> map = new HashMap<>();
        map.put(plugin.setting("slot_view.center", GUIVector.class, new GUIVector(4, 3)).toSlot(), v -> new ItemStack(Material.IRON_SWORD) /* TODO */);
        return map;
    }
}
