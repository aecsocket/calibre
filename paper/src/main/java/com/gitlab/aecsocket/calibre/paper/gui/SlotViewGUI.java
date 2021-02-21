package com.gitlab.aecsocket.calibre.paper.gui;

import com.gitlab.aecsocket.calibre.paper.util.item.ComponentCreationException;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.component.PaperSlot;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.unifiedframework.paper.gui.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;

public class SlotViewGUI extends GUI {
    public static final InventorySize SIZE = new InventorySize(6);
    public static final GUIVector CENTER = new GUIVector(4, 3);

    private final CalibrePlugin plugin;
    private PaperComponent component;
    private boolean modification;
    private boolean limited;
    private ItemSlot<BukkitItem> slot;
    private int amount;

    public SlotViewGUI(CalibrePlugin plugin, PaperComponent component, boolean modification, boolean limited, ItemSlot<BukkitItem> slot, int amount) {
        this.plugin = plugin;
        this.component = component;
        this.modification = modification;
        this.limited = limited;
        this.slot = slot;
        this.amount = amount;
    }

    public SlotViewGUI(CalibrePlugin plugin, PaperComponent component, boolean modification, boolean limited, ItemSlot<BukkitItem> slot) {
        this.plugin = plugin;
        this.component = component;
        this.modification = modification;
        this.limited = limited;
        this.slot = slot;
        amount = slot.get().amount();
    }

    public SlotViewGUI(CalibrePlugin plugin, PaperComponent component, boolean modification, boolean limited, int amount) {
        this.plugin = plugin;
        this.component = component;
        this.modification = modification;
        this.limited = limited;
        this.amount = amount;
    }

    public CalibrePlugin plugin() { return plugin; }

    public PaperComponent component() { return component; }
    public void component(PaperComponent component) { this.component = component; }

    public boolean modification() { return modification; }
    public void modification(boolean modification) { this.modification = modification; }

    public boolean limited() { return limited; }
    public void limited(boolean limited) { this.limited = limited; }

    public ItemSlot<BukkitItem> slot() { return slot; }
    public void slot(ItemSlot<BukkitItem> slot) { this.slot = slot; }

    public int amount() { return amount; }
    public void amount(int amount) { this.amount = amount; }

    @Override public GUIManager getGUIManager() { return plugin().guiManager(); }
    @Override public InventorySize getSize(Player player) { return SIZE; }
    @Override public net.kyori.adventure.text.Component getTitle(Player player) {
        return plugin.gen(player.getLocale(), "slot_view.title",
            "name", component.name(player.getLocale()));
    }

    @Override
    public Map<Integer, GUIItem> getItems(Player player) {
        Map<Integer, GUIItem> result = new HashMap<>();
        GUIVector vec;
        try {
            vec = plugin.setting("slot_view", "center").get(GUIVector.class, CENTER);
        } catch (SerializationException e) {
            vec = CENTER;
        }
        result.put(
                vec.slot(),
                new SlotViewItem(plugin, component, modification, limited, amount)
        );
        for (var entry : component.<PaperSlot>slots().entrySet())
            addItems(result, entry.getKey(), entry.getValue(), vec);
        return result;
    }

    private void addItems(Map<Integer, GUIItem> map, String slotKey, PaperSlot slot, GUIVector offset) {
        if (slot.offset() != null)
            offset = new GUIVector(offset.add(slot.offset()));
        map.put(offset.slot(), new SlotViewItem(plugin, slot, slotKey, modification, limited, amount));
        if (slot.get() == null)
            return;

        for (var entry : slot.get().<PaperSlot>slots().entrySet())
            addItems(map, entry.getKey(), entry.getValue(), offset);
    }

    @Override
    public GUIView createView(Player player) {
        slot.set(component.create(player.getLocale(), slot.get().amount()));
        return super.createView(player);
    }

    public void update(GUIView view) {
        if (slot == null)
            return;
        try {
            if (slot.get() == null) {
                view.getView().close();
                return;
            }
            PaperComponent realComponent = plugin.itemManager().get(slot.get().item());
            if (!component.equals(realComponent)) {
                component = realComponent;
                notifyUpdate(view);
            }
        } catch (ComponentCreationException e) {
            view.getView().close();
        }
    }

    @Override
    public void onClick(GUIView view, InventoryClickEvent event) {
        super.onClick(view, event);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, view::updateItems, 1);
    }

    public void notifyUpdate(GUIView view) {
        component.buildTree();
        if (slot != null)
            slot.set(component.create(view.getPlayer().getLocale(), slot.get().amount()));
        view.reopen();
    }
}
