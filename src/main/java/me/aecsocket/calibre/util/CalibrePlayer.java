package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.unifiedframework.event.Event;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.loop.Tickable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Wrapper around {@link Player} for storing Calibre's data.
 */
public class CalibrePlayer implements Tickable {
    private final CalibrePlugin plugin;
    private final Player player;

    public CalibrePlayer(CalibrePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public CalibrePlugin getPlugin() { return plugin; }
    public Player getPlayer() { return player; }

    private void callEvent(ItemStack stack, Event<?>... events) {
        CalibreItem item = plugin.getItem(stack, CalibreItem.class);
        if (item != null) {
            for (Event<?> event : events)
                item.callEvent(event);
        }
    }

    @Override
    public void tick(TickContext tickContext) {
        PlayerInventory inv = player.getInventory();
        callEvent(inv.getItemInMainHand(),
                new ItemEvents.Hold(
                        inv.getItemInMainHand(),
                        player,
                        EquipmentSlot.HAND));
        callEvent(inv.getItemInOffHand(),
                new ItemEvents.Hold(
                        inv.getItemInOffHand(),
                        player,
                        EquipmentSlot.OFF_HAND));
    }
}
