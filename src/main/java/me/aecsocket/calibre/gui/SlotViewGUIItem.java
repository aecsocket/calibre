package me.aecsocket.calibre.gui;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.util.ItemDescriptor;
import me.aecsocket.unifiedframework.gui.GUIItem;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.item.ItemCreationException;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlotViewGUIItem implements GUIItem {
    private final CalibrePlugin plugin;
    private final CalibreComponentSlot slot;
    private final String slotName;
    private CalibreComponent component;
    private boolean allowModification;
    private boolean limitedModification;

    public SlotViewGUIItem(CalibrePlugin plugin, CalibreComponentSlot slot, String slotName, CalibreComponent component, boolean allowModification, boolean limitedModification) {
        this.plugin = plugin;
        this.slot = slot;
        this.slotName = slotName;
        this.component = component;
        this.allowModification = allowModification;
        this.limitedModification = limitedModification;
    }

    public SlotViewGUIItem(SlotViewGUI gui, CalibreComponentSlot slot, String slotName, CalibreComponent component) {
        this(
                gui.getPlugin(), slot, slotName,
                slot == null
                        ? component
                        : slot.get() == null
                                ? null
                                : slot.get(),
                gui.allowsModification(), gui.limitsModification());
    }

    public SlotViewGUIItem(SlotViewGUI gui, CalibreComponentSlot slot, String slotName) {
        this(gui, slot, slotName, null);
    }

    public SlotViewGUIItem(SlotViewGUI gui, CalibreComponent component) {
        this(gui, null, null, component);
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public CalibreComponentSlot getSlot() { return slot; }
    public String getSlotName() { return slotName; }

    public CalibreComponent getComponent() { return component; }
    public void setComponent(CalibreComponent component) { this.component = component; }

    public boolean allowsModification() { return allowModification; }
    public void setAllowModification(boolean allowModification) { this.allowModification = allowModification; }

    public boolean limitsModification() { return limitedModification; }
    public void setLimitedModification(boolean limitedModification) { this.limitedModification = limitedModification; }

    @Override
    public ItemStack createItem(GUIView view) {
        Player player = view.getPlayer();
        if (slot != null && slot.get() == null)
            return createIcon(player, plugin.fromItem(view.getRawCursor()));
        else if (component != null) {
            ItemStack item;
            try {
                item = component.withSingleTree().createItem(player);
            } catch (ItemCreationException e) {
                item = Utils.modMeta(
                        plugin.setting("slot_view.icon.no_item", ItemDescriptor.class, new ItemDescriptor(Material.BARRIER, 0, 0))
                                .create(), meta -> meta.setDisplayName(component.getLocalizedName(player))
                );
            }
            if (slot != null)
                Utils.modMeta(item, meta -> {
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add(slot.getName(plugin, slotName, plugin.locale(player)));
                    meta.setLore(lore);
                });
            return item;
        } else
            return null;
    }

    public ItemStack createIcon(Player player, @Nullable CalibreComponent activeComponent) {
        return createIcon(plugin, slot, slotName, player, activeComponent);
    }

    @Override
    public void onClick(GUIView view, InventoryClickEvent event) {
        event.setCancelled(true);

        if (slot == null) return;
        if (!allowModification || (limitedModification && !slot.canFieldModify())) return;

        Player player = view.getPlayer();
        ItemStack rawCursor = view.getRawCursor();
        if (Utils.empty(rawCursor) && slot.get() != null) {
            if (event.getClick() == ClickType.RIGHT) {
                // TODO component editing menu
                return;
            }

            try {
                CalibreComponent component = slot.get().withSimpleTree();
                view.setRawCursor(component.createItem(player));
                slot.set(null);
                SoundData.play(player::getLocation, component.stat("slot_view_remove"));
            } catch (ItemCreationException e) {
                return;
            }
        } else if (!Utils.empty(rawCursor)) {
            CalibreComponent component = plugin.fromItem(rawCursor);
            if (component == null || !slot.isCompatible(component)) return;

            try {
                if (slot.get() == null)
                    view.setRawCursor(rawCursor.subtract());
                else if (rawCursor.getAmount() > 1)
                    return;
                else
                    view.setRawCursor(slot.get().withSimpleTree().createItem(player));
                slot.set(component);
                SoundData.play(player::getLocation, component.stat("slot_view_add"));
            } catch (ItemCreationException e) {
                return;
            }
        } else
            return;

        event.setCancelled(false);
        ((SlotViewGUI) view.getGUI()).notifyUpdate(view);
    }

    public static ItemStack createIcon(CalibrePlugin plugin, CalibreComponentSlot slot, String slotName, Player player, @Nullable CalibreComponent activeComponent) {
        boolean required = slot.isRequired();
        return Utils.modMeta(plugin.setting("slot_view.icon." + (
                activeComponent == null
                ? required
                        ? "required"
                        : "normal"
                : slot.isCompatible(activeComponent)
                        ? "compatible"
                        : "incompatible"
                ), ItemDescriptor.class, new ItemDescriptor(Material.GRAY_STAINED_GLASS_PANE, 0, 0)).create(), meta -> {
            meta.setDisplayName(plugin.gen("slot." + slotName));
            if (slot.canFieldModify())
                meta.setLore(Collections.singletonList(plugin.gen(player, "slot_view.can_field_modify")));
        });
    }
}
