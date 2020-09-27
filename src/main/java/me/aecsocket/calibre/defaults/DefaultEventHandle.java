package me.aecsocket.calibre.defaults;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.gui.SlotViewGUI;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link CalibreDefaultHook}'s event handler.
 */
public class DefaultEventHandle implements Listener {
    private final CalibrePlugin plugin;
    private final CalibreDefaultHook hook;

    public DefaultEventHandle(CalibrePlugin plugin, CalibreDefaultHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public CalibreDefaultHook getHook() { return hook; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GUIView view = hook.getGUIManager().getView(event.getWhoClicked());
        if (view != null && view.getGUI() instanceof SlotViewGUI) {
            SlotViewGUI gui = (SlotViewGUI) view.getGUI();
            if (event.getClickedInventory() == event.getView().getTopInventory() || gui.getItemSlot() == event.getSlot() || gui.getItemSlot() == event.getHotbarButton()) {
                event.setCancelled(true);
                return;
            }
        }

        CalibreComponent root = plugin.getItem(event.getCurrentItem(), CalibreComponent.class);
        if (root == null) return;
        ItemStack cursorStack = event.getCursor();
        if (
                !Utils.empty(cursorStack)
                && plugin.setting("quick_modify.enable", boolean.class, true)
                && player.getGameMode() != GameMode.CREATIVE
                && event.getCurrentItem().getAmount() == 1
        ) {
            CalibreComponent cursor = plugin.getItem(cursorStack, CalibreComponent.class);
            if (cursor != null) {
                if (root.combine(cursor, plugin.setting("quick_modify.limited_modification", boolean.class, true)) != null) {
                    event.getView().setCursor(cursorStack.subtract());
                    hook.updateSlotView(player, root);
                    event.setCurrentItem(root.createItem(player));
                    SoundData.play(player, plugin.setting("quick_modify.sound", SoundData[].class, null));
                    event.setCancelled(true);
                }
            }
        } else if (
                event.getClick() == ClickType.RIGHT
                && plugin.setting("field_modify.enable", boolean.class, true)
        ) {
            if (root.getSlots().size() > 0 || plugin.setting("field_modify.enable_for_simple", boolean.class, false)) {
                event.setCancelled(true);
                new SlotViewGUI(
                        plugin,
                        hook.getGUIManager(),
                        root,
                        plugin.setting("field_modify.enable_modification", boolean.class, true)
                                && event.getClickedInventory() == event.getView().getBottomInventory()
                                && event.getCurrentItem().getAmount() == 1,
                        plugin.setting("field_modify.limited_modification", boolean.class, true),
                        player,
                        event.getSlot())
                        .open(player);
            }
        }
    }
}
