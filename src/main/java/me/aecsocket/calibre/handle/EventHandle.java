package me.aecsocket.calibre.handle;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.gui.SlotViewGUI;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.util.slot.EntityItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.slot.PlayerItemSlot;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.unifiedframework.gui.GUIView;
import me.aecsocket.unifiedframework.util.data.SoundData;
import org.bukkit.Bukkit;
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
    private int cannotInteract;
    private int cannotDrop;

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
        if (cannotInteract == Bukkit.getCurrentTick()) return;
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
    public void onDrop(PlayerDropItemEvent event) {
        cannotInteract = Bukkit.getCurrentTick();
        callEvent(event.getItemDrop().getItemStack(), ItemEvents.BukkitDrop.of(plugin, event));
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
        cannotInteract = Bukkit.getCurrentTick();
        cannotDrop = Bukkit.getCurrentTick();
        Player player = (Player) event.getWhoClicked();
        PlayerItemUser user = plugin.userOf(player);
        PlayerInventory inv = player.getInventory();
        EntityItemSlot handSlot = new EntityItemSlot(player, EquipmentSlot.HAND);

        GUIView view = plugin.getGUIManager().getView(player);
        SlotViewGUI gui = view == null
                ? null
                : view.getGUI() instanceof SlotViewGUI
                ? (SlotViewGUI) view.getGUI()
                : null;
        boolean guiClicked = gui != null && event.getClickedInventory() == event.getView().getTopInventory();
        if (!guiClicked && plugin.setting("slot_view.enabled", boolean.class, true) && event.getClick() == ClickType.RIGHT) {
            CalibreComponent component = plugin.fromItem(event.getCurrentItem());
            if (component == null) return;
            boolean allowModification = plugin.setting("slot_view.allow_modification", boolean.class, true);
            new SlotViewGUI(
                    plugin, component,
                    allowModification,
                    allowModification
                            ? new ItemSlot() {
                                @Override public ItemStack get() { return event.getCurrentItem(); }
                                @Override public void set(ItemStack item) { event.setCurrentItem(item); }
                            }
                            : null
            ).open((Player) event.getWhoClicked());
            return;
        }

        if (
                plugin.setting("quick_modify.enabled", boolean.class, true)
                && event.getClick() == ClickType.LEFT
                && (gui == null || event.getClickedInventory() != event.getView().getTopInventory())
        ) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getAmount() > 1) return;
            CalibreComponent base = plugin.fromItem(event.getCurrentItem());
            CalibreComponent mod = plugin.fromItem(event.getCursor());
            if (base == null || mod == null) return;
            boolean updateGui = gui != null && gui.getSlot() != null && event.getCurrentItem().equals(gui.getSlot().get());
            if (base.combine(mod, plugin.setting("quick_modify.limited_modification", boolean.class, true)) != null) {
                SoundData.play(player::getLocation, base.stat("quick_modify"));
                base.updateItem(plugin.userOf(player), new PlayerItemSlot(player, event.getSlot()));
                event.getCursor().subtract();
                event.setCancelled(true);

                if (updateGui) {
                    gui.setComponent(base);
                    gui.notifyUpdate(view);
                }
                return;
            }
        }

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
}
