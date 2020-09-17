package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.unifiedframework.event.Event;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

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
        if (plugin.getPlayerData(event.getPlayer()).getLastClicked() == Bukkit.getCurrentTick())
            return;
        callEvent(event.getItem(),
                new ItemEvents.BukkitInteract<>(event).toRaw(),
                new ItemEvents.BukkitInteract<>(event));
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        callEvent(event.getOffHandItem(),
                new ItemEvents.BukkitSwapHands<>(event, true).toRaw(),
                new ItemEvents.BukkitSwapHands<>(event, true));
        callEvent(event.getMainHandItem(),
                new ItemEvents.BukkitSwapHands<>(event, false).toRaw(),
                new ItemEvents.BukkitSwapHands<>(event, false));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity)) return;
        EntityEquipment equipment = ((LivingEntity) event.getDamager()).getEquipment();
        callEvent(equipment.getItemInMainHand(),
                new ItemEvents.BukkitDamage<>(event, true).toRaw(),
                new ItemEvents.BukkitDamage<>(event, true));
        callEvent(equipment.getItemInOffHand(),
                new ItemEvents.BukkitDamage<>(event, false).toRaw(),
                new ItemEvents.BukkitDamage<>(event, false));
    }

    @EventHandler
    public void onSwapItem(PlayerItemHeldEvent event) {
        plugin.getPlayerData(event.getPlayer()).setAnimation(null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        plugin.getPlayerData((Player) event.getWhoClicked()).setLastClicked(Bukkit.getCurrentTick());
    }
}
