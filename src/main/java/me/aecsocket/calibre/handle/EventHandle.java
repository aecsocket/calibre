package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.util.damagecause.BukkitDamageCause;
import me.aecsocket.calibre.item.util.slot.EntityItemSlot;
import me.aecsocket.calibre.item.util.slot.PlayerItemSlot;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EventHandle implements Listener {
    private final CalibrePlugin plugin;

    public EventHandle(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    private void callEvent(ItemStack stack, Object... events) {
        CalibreComponent component = plugin.fromItem(stack);
        if (component != null) {
            for (Object event : events)
                component.callEvent(event);
        }
    }

    @EventHandler
    public void onSwitchItem(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();

        ItemStack item = inv.getItem(event.getPreviousSlot());
        callEvent(item, ItemEvents.BukkitHolster.of(plugin, event));
        item = inv.getItem(event.getNewSlot());
        callEvent(item, ItemEvents.BukkitDraw.of(plugin, event));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        PlayerItemUser user = plugin.userOf(player);
        PlayerInventory inv = player.getInventory();
        EntityItemSlot handSlot = new EntityItemSlot(player, EquipmentSlot.HAND);
        if (event.getSlot() == inv.getHeldItemSlot()) {
            // Must have holster before draw so that, in case both items the event is called on are the same item,
            // the last event ran is a draw event
            ItemStack item = inv.getItem(event.getSlot());
            callEvent(item, new ItemEvents.Holster(item, handSlot, user));

            item = event.getCursor();
            callEvent(item, new ItemEvents.Draw(item, handSlot, user));
        }

        if (event.getHotbarButton() == inv.getHeldItemSlot()) {
            ItemStack item = inv.getItem(event.getHotbarButton());
            callEvent(item, new ItemEvents.Holster(item, handSlot, user));

            item = inv.getItem(event.getSlot());
            callEvent(item, new ItemEvents.Draw(item, handSlot, user));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = entity.getEquipment().getItem(slot);
                callEvent(item, ItemEvents.BukkitDamage.of(plugin, event, slot));
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getDamager();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = entity.getEquipment().getItem(slot);
                callEvent(item, ItemEvents.BukkitAttack.of(plugin, event, slot));
            }
        }
    }
}
