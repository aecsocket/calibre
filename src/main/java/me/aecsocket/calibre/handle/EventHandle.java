package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.unifiedframework.event.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * The plugin's event handler.
 */
public class EventHandle implements Listener {
    private final CalibrePlugin plugin;

    public EventHandle(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    private void callEvent(ItemStack stack, Event<?>... events) {
        CalibreItem item = plugin.getItem(stack, CalibreItem.class);
        if (item != null) {
            for (Event<?> event : events)
                item.callEvent(event);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        callEvent(event.getItem(),
                new ItemEvents.Interact(
                    event.getItem(),
                    event.getPlayer(),
                    event.getHand(),
                    event.getBlockFace(),
                    event.getClickedBlock()),
                new ItemEvents.BukkitInteract(event));
    }
}
