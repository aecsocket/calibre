package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.gui.SlotViewGUI;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.util.slot.EntityItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
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
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        EntityEquipment equipment = event.getPlayer().getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = equipment.getItem(slot);
            ItemEvents.Interact iEvent = ItemEvents.BukkitInteract.of(plugin, event, slot);
            callEvent(item, iEvent);
            event.setUseItemInHand(iEvent.getHandResult());
            event.setUseInteractedBlock(iEvent.getBlockResult());
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        EntityEquipment equipment = event.getPlayer().getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values())
            callEvent(equipment.getItem(slot), ItemEvents.BukkitSwapHand.of(plugin, event, slot));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = entity.getEquipment().getItem(slot);
                ItemEvents.Damage iEvent = ItemEvents.BukkitDamage.of(plugin, event, slot);
                callEvent(item, iEvent);
                if (iEvent.getDamage() <= 0) event.setCancelled(true);
                event.setDamage(iEvent.getDamage());
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getDamager();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = entity.getEquipment().getItem(slot);
                ItemEvents.Attack iEvent = ItemEvents.BukkitAttack.of(plugin, event, slot);
                callEvent(item, iEvent);
                if (iEvent.getDamage() <= 0) event.setCancelled(true);
                event.setDamage(iEvent.getDamage());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        EntityEquipment equipment = event.getPlayer().getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = equipment.getItem(slot);
            callEvent(item, new ItemEvents.Draw(
                    item, new EntityItemSlot(event.getPlayer(), slot), plugin.userOf(event.getPlayer())
            ));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayers().remove(event.getPlayer());
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

        if (!plugin.setting("slot_view.enabled", boolean.class, true)) return;
        if (event.getClick() != ClickType.RIGHT) return;
        CalibreComponent component = plugin.fromItem(event.getCurrentItem());
        if (component == null) return;
        new SlotViewGUI(
                plugin, plugin.getGUIManager(), component,
                plugin.setting("slot_view.allow_modification", boolean.class, true),
                plugin.setting("slot_view.limited_modification", boolean.class, true),
                // todo broken?
                new ItemSlot() {
                    @Override public ItemStack get() { return event.getCurrentItem(); }
                    @Override public void set(ItemStack item) { event.setCurrentItem(item); }
                }
        ).open((Player) event.getWhoClicked());
    }
}
