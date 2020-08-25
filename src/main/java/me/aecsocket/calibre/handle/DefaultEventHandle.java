package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.gui.SlotViewGUI;
import me.aecsocket.calibre.hook.CalibreDefaultHook;
import me.aecsocket.calibre.item.component.CalibreComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * The {@link me.aecsocket.calibre.hook.CalibreDefaultHook}'s event handler.
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
        if (event.getClick() != ClickType.RIGHT) return;
        if (!plugin.setting("slot_view.enable", boolean.class, true)) return;

        CalibreComponent component = plugin.getItem(event.getCurrentItem(), CalibreComponent.class);
        if (component != null)
            new SlotViewGUI(plugin, hook.getGUIManager(), component).open((Player) event.getWhoClicked());
    }
}
