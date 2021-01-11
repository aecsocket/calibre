package me.aecsocket.calibre;

import me.aecsocket.calibre.component.CalibreSlot;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.gui.SlotViewGUI;
import me.aecsocket.calibre.system.BukkitItemEvents;
import me.aecsocket.calibre.wrapper.slot.BukkitSlot;
import me.aecsocket.unifiedframework.gui.GUIView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CalibreListener implements Listener {
    private final CalibrePlugin plugin;

    public CalibreListener(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEvent(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            PaperComponent component = plugin.getComponentOrNull(event.getPlayer().getInventory().getItem(slot));
            if (component == null)
                return;
            component.tree().call(BukkitItemEvents.BukkitInteract.of(event, component, slot));
        }
    }

    @EventHandler
    public void onEvent(InventoryClickEvent event) {
        ItemStack clickedStack = event.getCurrentItem();
        ItemStack cursorStack = event.getCursor();
        PaperComponent clicked = plugin.getComponentOrNull(clickedStack);
        PaperComponent cursor = plugin.getComponentOrNull(cursorStack);
        ClickType type = event.getClick();
        Player player = (Player) event.getWhoClicked();

        GUIView view = plugin.getGUIManager().getView(player);
        if (view != null && view.getGUI() instanceof SlotViewGUI) {
            if (event.getClickedInventory() == event.getView().getTopInventory())
                return;
        }

        if (clicked == null)
            return;
        if (clicked.tree().call(BukkitItemEvents.BukkitClick.of(event, clicked)).cancelled())
            return;

        if (cursor != null && type == ClickType.LEFT && plugin.setting("quick_modify", "enabled").getBoolean(true)) {
            int clickedAmt = clickedStack.getAmount();
            if (clickedAmt == 1 || clickedAmt == cursorStack.getAmount()) {
                CalibreSlot slot = clicked.combine(cursor, plugin.setting("quick_modify", "limited").getBoolean(true));
                if (slot != null) {
                    event.setCurrentItem(clicked.create(player.getLocale(), clickedStack));
                    event.getView().setCursor(cursorStack.subtract(clickedAmt));
                    event.setCancelled(true);
                }
            }
            return;
        }

        if (cursor == null && type == ClickType.RIGHT && plugin.setting("slot_view", "enabled").getBoolean(true)) {
            event.setCancelled(true);
            new SlotViewGUI(
                    plugin, clicked,
                    plugin.setting("slot_view", "modification").getBoolean(true),
                    plugin.setting("slot_view", "limited").getBoolean(true),
                    BukkitSlot.of(event::getCurrentItem, event::setCurrentItem)
            ).open(player);
        }
    }
}
