package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.util.itemuser.PlayerItemUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
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

    private void callEvent(ItemStack stack, Object... events) {
        CalibreItem item = plugin.getItem(stack, CalibreItem.class);
        if (item != null) {
            for (Object event : events)
                item.callEvent(event);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getPlayerData(event.getPlayer()).getLastClicked() == Bukkit.getCurrentTick())
            return;
        callEvent(event.getItem(),
                new ItemEvents.BukkitInteract(plugin, event).toRaw(),
                new ItemEvents.BukkitInteract(plugin, event));
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        callEvent(event.getOffHandItem(),
                new ItemEvents.BukkitSwapHands(plugin, event, true).toRaw(),
                new ItemEvents.BukkitSwapHands(plugin, event, true));
        callEvent(event.getMainHandItem(),
                new ItemEvents.BukkitSwapHands(plugin, event, false).toRaw(),
                new ItemEvents.BukkitSwapHands(plugin, event, false));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity)) return;
        EntityEquipment equipment = ((LivingEntity) event.getDamager()).getEquipment();
        callEvent(equipment.getItemInMainHand(),
                new ItemEvents.BukkitDamage(plugin, event, true).toRaw(),
                new ItemEvents.BukkitDamage(plugin, event, true));
        callEvent(equipment.getItemInOffHand(),
                new ItemEvents.BukkitDamage(plugin, event, false).toRaw(),
                new ItemEvents.BukkitDamage(plugin, event, false));
    }

    @EventHandler
    public void onSwapItem(PlayerItemHeldEvent event) {
        plugin.getPlayerData(event.getPlayer()).setAnimation(null);
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        callEvent(inv.getItem(event.getNewSlot()),
                new ItemEvents.BukkitDraw(plugin, event).toRaw(),
                new ItemEvents.BukkitDraw(plugin, event));
        callEvent(inv.getItem(event.getPreviousSlot()),
                new ItemEvents.Holster(
                        inv.getItem(event.getPreviousSlot()),
                        EquipmentSlot.HAND,
                        new PlayerItemUser(plugin, player),
                        event.getPreviousSlot()
                ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(InventoryClickEvent event) {
        plugin.getPlayerData((Player) event.getWhoClicked()).setLastClicked(Bukkit.getCurrentTick());

        if (event.isCancelled()) return;
        Player player = (Player) event.getWhoClicked();
        PlayerInventory inv = player.getInventory();
        int slot = event.getSlot();
        int hbarSlot = event.getHotbarButton();
        if (slot == inv.getHeldItemSlot())
            callDrawHolsterEvents(inv.getItem(slot), event.getCursor(), slot, player);
        if (event.getClick() == ClickType.NUMBER_KEY && hbarSlot == inv.getHeldItemSlot())
            callDrawHolsterEvents(inv.getItem(hbarSlot), inv.getItem(slot), hbarSlot, player);
    }

    private void callDrawHolsterEvents(ItemStack from, ItemStack to, int slot, Player player) {
        callEvent(from,
                new ItemEvents.Holster(
                        from,
                        EquipmentSlot.HAND,
                        new PlayerItemUser(plugin, player),
                        slot
                ));
        callEvent(to,
                new ItemEvents.Draw(
                        to,
                        EquipmentSlot.HAND,
                        new PlayerItemUser(plugin, player),
                        slot
                ));
    }
}
