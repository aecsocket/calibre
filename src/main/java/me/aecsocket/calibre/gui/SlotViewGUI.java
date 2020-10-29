package me.aecsocket.calibre.gui;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.unifiedframework.gui.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SlotViewGUI extends GUI {
    public static final InventorySize INVENTORY_SIZE = new InventorySize(6);

    private final CalibrePlugin plugin;
    private final GUIManager guiManager;
    private CalibreComponent component;
    private boolean allowModification;
    private boolean limitedModification;
    private ItemSlot slot;


    public SlotViewGUI(CalibrePlugin plugin, GUIManager guiManager, CalibreComponent component, boolean allowModification, boolean limitedModification, ItemSlot slot) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.component = component;
        this.allowModification = allowModification;
        this.limitedModification = limitedModification;
        this.slot = slot;
    }

    public CalibrePlugin getPlugin() { return plugin; }
    @Override public GUIManager getGUIManager() { return guiManager; }

    public CalibreComponent getComponent() { return component; }
    public void setComponent(CalibreComponent component) { this.component = component; }

    public boolean allowsModification() { return allowModification; }
    public void setAllowModification(boolean allowModification) { this.allowModification = allowModification; }

    public boolean limitsModification() { return limitedModification; }
    public void setLimitedModification(boolean limitedModification) { this.limitedModification = limitedModification; }

    public ItemSlot getSlot() { return slot; }
    public void setSlot(ItemSlot slot) { this.slot = slot; }

    @Override public InventorySize getSize(Player player) { return INVENTORY_SIZE; }

    @Override
    public String getTitle(Player player) {
        return plugin.gen(player, "slot_view.title",
            "name", component.getLocalizedName(player));
    }

    @Override
    public Map<Integer, GUIItem> getItems(Player player) {
        Map<Integer, GUIItem> map = new HashMap<>();
        GUIVector center = plugin.setting("slot_view.center", GUIVector.class, new GUIVector(4, 3));
        map.put(center.toSlot(), new SlotViewGUIItem(this, component.withSimpleTree()));
        component.getSlots().forEach((slotName, slot) -> putItems(map, slot, slotName, center));
        return map;
    }

    private void putItems(Map<Integer, GUIItem> map, CalibreComponentSlot slot, String slotName, GUIVector vec) {
        if (slot.getOffset() != null)
            vec = vec.clone().add(slot.getOffset());
        map.put(vec.toSlot(), new SlotViewGUIItem(this, slot, slotName));
        if (slot.get() == null) return;

        CalibreComponent component = slot.get();
        final GUIVector vec2 = vec;
        component.getSlots().forEach((childName, child) -> putItems(map, child, childName, vec2));
    }

    public boolean check(GUIView view) {
        if (slot == null) return true;
        ItemStack expectedItem = component.createItem(view.getPlayer());
        return expectedItem.isSimilar(slot.get());
    }

    public void validate(GUIView view) {
        if (!check(view))
            view.getView().close();
    }

    public void notifyUpdate(GUIView view) {
        component = component.withSimpleTree();
        if (slot != null)
            slot.set(component.createItem(view.getPlayer(), slot.get().getAmount()));
        view.reopen();
    }

    @Override
    public void onClick(GUIView view, InventoryClickEvent event) {
        super.onClick(view, event);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, view::updateItems, 1);
    }
}
