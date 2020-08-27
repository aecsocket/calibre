package me.aecsocket.calibre.defaults.gui;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.unifiedframework.gui.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

/**
 * A GUI representing the slots of a {@link CalibreComponent}. On every slot update such as taking a component out,
 * the GUI is validated through {@link SlotViewGUI#validate(GUIView)}, and if it does not pass the GUI closes.
 * This prevents duplication exploits.
 */
public class SlotViewGUI extends GUI {
    public static final InventorySize INVENTORY_SIZE = new InventorySize(6);

    private final CalibrePlugin plugin;
    private final GUIManager guiManager;
    private CalibreComponent component;
    private boolean allowModification;

    private Player player;
    private int clickedSlot;
    private ItemStack existingStack;

    public SlotViewGUI(CalibrePlugin plugin, GUIManager guiManager, CalibreComponent component, boolean allowModification, Player player, int clickedSlot) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.component = component;
        this.allowModification = allowModification;

        this.player = player;
        this.clickedSlot = clickedSlot;
        existingStack = player.getInventory().getItem(clickedSlot);
    }

    public SlotViewGUI(CalibrePlugin plugin, GUIManager guiManager, CalibreComponent component, boolean allowModification) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.component = component;
        this.allowModification = allowModification;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    public CalibreComponent getComponent() { return component; }
    public void setComponent(CalibreComponent component) { this.component = component; }

    public boolean allowsModification() { return allowModification; }
    public void setAllowModification(boolean allowModification) { this.allowModification = allowModification; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public int getClickedSlot() { return clickedSlot; }
    public void setClickedSlot(int clickedSlot) { this.clickedSlot = clickedSlot; }

    public ItemStack getExistingStack() { return existingStack; }
    public void setExistingStack(ItemStack existingStack) { this.existingStack = existingStack; }

    @Override public GUIManager getGUIManager() { return guiManager; }
    @Override public InventorySize getSize(Player player) { return INVENTORY_SIZE; }
    @Override public String getTitle(Player player) { return plugin.gen(player, "gui.slot_view"); }

    @Override
    public Map<Integer, GUIItem> getItems(Player player) {
        Map<Integer, GUIItem> map = new HashMap<>();
        GUIVector center = plugin.setting("slot_view.center", GUIVector.class, new GUIVector(4, 3));
        map.put(center.toSlot(), v -> component.treeless().createItem(player));
        component.getSlots().forEach((name, slot) -> putItems(map, name, slot, center));
        return map;
    }

    @Override
    public void onClick(GUIView view, InventoryClickEvent event) {
        super.onClick(view, event);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, view::updateItems, 1);
    }

    private void putItems(Map<Integer, GUIItem> map, String name, CalibreComponentSlot slot, GUIVector vec) {
        vec = vec.clone().add(slot.getOffset());
        map.put(vec.toSlot(), new SlotViewGUIItem(this, slot, name));
        if (slot.get() == null) return;

        CalibreComponent component = slot.get();
        final GUIVector vec2 = vec;
        component.getSlots().forEach((childName, child) -> putItems(map, childName, child, vec2));
    }

    public boolean validate(GUIView view) {
        if (player == null || existingStack == null) return true;
        if (!existingStack.equals(player.getInventory().getItem(clickedSlot))) {
            view.getView().close();
            return false;
        }
        return true;
    }

    public void notifyModification(GUIView view) {
        if (!validate(view)) return;

        view.reopen();
        if (component.getTree() != null) component.getTree().rebuild();

        if (player != null && existingStack != null) {
            PlayerInventory inv = player.getInventory();
            existingStack = component.createItem(player, inv.getItem(clickedSlot).getAmount());
            inv.setItem(clickedSlot, existingStack);
        }
    }
}
