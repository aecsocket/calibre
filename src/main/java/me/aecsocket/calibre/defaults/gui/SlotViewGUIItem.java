package me.aecsocket.calibre.defaults.gui;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.unifiedframework.gui.GUIItem;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SlotViewGUIItem implements GUIItem {
    private final SlotViewGUI gui;
    private CalibreComponentSlot slot;
    private String name;
    private boolean allowModification;
    private boolean limitedModification;

    public SlotViewGUIItem(SlotViewGUI gui, CalibreComponentSlot slot, String name) {
        this.gui = gui;
        this.slot = slot;
        this.name = name;
        this.allowModification = gui.allowsModification();
        this.limitedModification = gui.isLimitedModification();
    }

    public SlotViewGUI getGUI() { return gui; }

    public CalibreComponentSlot getSlot() { return slot; }
    public void setSlot(CalibreComponentSlot slot) { this.slot = slot; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean allowsModification() { return allowModification; }
    public void setAllowModification(boolean allowModification) { this.allowModification = allowModification; }

    public boolean isLimitedModification() { return limitedModification; }
    public void setLimitedModification(boolean limitedModification) { this.limitedModification = limitedModification; }

    @Override
    public ItemStack createItem(GUIView view) {
        Player player = view.getPlayer();
        if (slot.get() == null)
            return slot.createIcon(player, name, gui.getPlugin().getItem(view.getRawCursor(), CalibreComponent.class));
        else {
            return Utils.modMeta(slot.get().createItem(player), meta -> {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(gui.getPlugin().gen(player, "slot." + name));
                meta.setLore(lore);
            });
        }
    }

    @Override
    public void onClick(GUIView view, InventoryClickEvent event) {
        if (!allowModification || (limitedModification && !slot.canFieldModify())) {
            event.setCancelled(true);
            return;
        }

        CalibrePlugin plugin = gui.getPlugin();
        if (!gui.validate(view)) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursor = view.getRawCursor();
        Player player = view.getPlayer();
        if (Utils.empty(cursor) && slot.get() != null) {
            view.setRawCursor(slot.get().createItem(view.getPlayer()));
            slot.set(null);
            SoundData.play(player, plugin.setting("slot_view.remove_sound", SoundData[].class, null));
        } else if (!Utils.empty(cursor)) {
            CalibreComponent component = plugin.getItem(cursor, CalibreComponent.class);
            if (component == null || !slot.isCompatible(component)) {
                event.setCancelled(true);
                return;
            }

            view.setRawCursor(slot.get() == null
                    ? cursor.subtract()
                    : slot.get().createItem(player));
            slot.set(component);
            SoundData.play(player, plugin.setting("slot_view.place_sound", SoundData[].class, null));
        } else {
            event.setCancelled(true);
            return;
        }

        gui.notifyModification(view);
    }
}
