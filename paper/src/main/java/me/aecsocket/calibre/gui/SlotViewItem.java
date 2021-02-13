package me.aecsocket.calibre.gui;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.component.PaperSlot;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.unifiedframework.gui.GUIItem;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import me.aecsocket.unifiedframework.util.data.SoundData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SlotViewItem implements GUIItem {
    private final CalibrePlugin plugin;
    private final PaperSlot slot;
    private final String slotKey;
    private final CalibreComponent<BukkitItem> component;
    private final boolean modification;
    private final boolean limited;
    private final int amount;

    public SlotViewItem(CalibrePlugin plugin, PaperSlot slot, String slotKey, CalibreComponent<BukkitItem> component, boolean modification, boolean limited, int amount) {
        this.plugin = plugin;
        this.slot = slot;
        this.slotKey = slotKey;
        this.component = component;
        this.modification = modification;
        this.limited = limited;
        this.amount = amount;
    }

    public SlotViewItem(CalibrePlugin plugin, PaperSlot slot, String slotKey, boolean modification, boolean limited, int amount) {
        this.plugin = plugin;
        this.slot = slot;
        this.slotKey = slotKey;
        this.component = slot.get();
        this.modification = modification;
        this.limited = limited;
        this.amount = amount;
    }

    public SlotViewItem(CalibrePlugin plugin, CalibreComponent<BukkitItem> component, boolean modification, boolean limited, int amount) {
        this.plugin = plugin;
        slot = null;
        slotKey = null;
        this.component = component;
        this.modification = modification;
        this.limited = limited;
        this.amount = amount;
    }

    public CalibrePlugin plugin() { return plugin; }

    public PaperSlot slot() { return slot; }
    public String slotKey() { return slotKey; }
    public CalibreComponent<?> component() { return component; }
    public boolean modification() { return modification; }
    public boolean limited() { return limited; }
    public int amount() { return amount; }

    @Override
    public ItemStack createItem(GUIView view) {
        CalibreComponent<BukkitItem> component = this.component;
        if (component == null) {
            if (slot == null)
                return null;
            else
                component = slot.get();
        }

        CalibreComponent<BukkitItem> cursor = plugin.itemManager().get(view.getView().getCursor());

        String locale = view.getPlayer().getLocale();
        ItemStack item;
        if (component == null) {
            item = slot.createViewItem(locale, slotKey, cursor);
        } else {
            CalibreComponent<BukkitItem> copy = component.copy();
            copy.slots().clear();
            ComponentTree tree = new ComponentTree(copy).buildTree();
            tree.complete(false);
            tree.buildStats();
            item = copy.create(locale).item();
        }

        if (item != null && slot != null && slot.fieldModifiable())
            BukkitUtils.modMeta(item, meta -> {
                List<String> lore = meta.getLore();
                if (lore == null)
                    lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().serialize(plugin.gen(locale, "slot_view.field_modifiable")));
                meta.setLore(lore);
            });
        return item;
    }

    @Override
    public void onClick(GUIView view, InventoryClickEvent event) {
        event.setCancelled(true);
        if (slot == null)
            return;
        if (!modification || (limited && !slot.fieldModifiable()))
            return;

        Player player = view.getPlayer();
        String locale = player.getLocale();
        ItemStack rawCursor = view.getRawCursor();
        if (BukkitUtils.empty(rawCursor) && slot.get() != null) {
            // remove component
            CalibreComponent<BukkitItem> inSlot = slot.<CalibreComponent<BukkitItem>>get().buildTree();
            SoundData.play(player::getLocation, inSlot.tree().stat("remove_sound"));
            view.setRawCursor(inSlot.create(locale, amount).item());
            slot.set(null);
        } else if (!BukkitUtils.empty(rawCursor)) {
            // insert
            if (rawCursor.getAmount() < amount)
                // prevents item loss/duplication
                return;
            CalibreComponent<BukkitItem> component = plugin.itemManager().get(rawCursor);
            if (component == null || !slot.isCompatible(component))
                return;

            SoundData.play(player::getLocation, component.tree().stat("insert_sound"));
            if (slot.get() == null)
                rawCursor.subtract(amount);
            else if (amount == rawCursor.getAmount())
                view.setRawCursor(slot.<CalibreComponent<BukkitItem>>get().buildTree().create(locale, amount).item());
            slot.set(component);
        } else
            return;

        event.setCancelled(false);
        if (view.getGUI() instanceof SlotViewGUI)
            ((SlotViewGUI) view.getGUI()).notifyUpdate(view);
    }
}
