package me.aecsocket.calibre.system;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.world.ItemSlot;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.calibre.wrapper.slot.BukkitSlot;
import me.aecsocket.calibre.wrapper.slot.EntityEquipmentSlot;
import me.aecsocket.calibre.wrapper.user.PlayerUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class BukkitItemEvents {
    private BukkitItemEvents() {}

    public static class BukkitEquipped extends ItemEvents.Equipped<BukkitItem> {
        private final TickContext tickContext;

        public BukkitEquipped(CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, TickContext tickContext) {
            super(component, user, slot);
            this.tickContext = tickContext;
        }

        public TickContext tickContext() { return tickContext; }

        public static BukkitEquipped of(PaperComponent component, Player player, EquipmentSlot slot, TickContext tickContext) {
            return new BukkitEquipped(component, PlayerUser.of(player), EntityEquipmentSlot.of(player, slot), tickContext);
        }
    }

    public static class BukkitInteract extends ItemEvents.Interact<BukkitItem> {
        private final PlayerInteractEvent event;

        public BukkitInteract(PlayerInteractEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, int type) {
            super(component, user, slot, type);
            this.event = event;
        }

        public PlayerInteractEvent event() { return event; }

        @Override public boolean cancelled() { return event.useItemInHand() == Event.Result.DENY; }
        @Override public void cancel() { event.setUseItemInHand(Event.Result.DENY); }

        public static BukkitInteract of(PlayerInteractEvent event, PaperComponent component, EquipmentSlot slot) {
            Player player = event.getPlayer();
            int type;
            switch (event.getAction()) {
                case LEFT_CLICK_AIR:
                case LEFT_CLICK_BLOCK:
                    type = ItemEvents.Interact.LEFT;
                    break;
                case RIGHT_CLICK_AIR:
                case RIGHT_CLICK_BLOCK:
                    type = ItemEvents.Interact.RIGHT;
                    break;
                default:
                    throw new IllegalArgumentException("Incompatible action type " + event.getAction());
            }
            return new BukkitInteract(
                    event,
                    component,
                    PlayerUser.of(player),
                    EntityEquipmentSlot.of(player, slot),
                    type
            );
        }
    }

    public static class BukkitClick extends ItemEvents.Click<BukkitItem> {
        private final InventoryClickEvent event;

        public BukkitClick(InventoryClickEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, ItemSlot<BukkitItem> cursor, boolean leftClick, boolean rightClick, boolean shiftClick) {
            super(component, user, slot, cursor, leftClick, rightClick, shiftClick);
            this.event = event;
        }

        public InventoryClickEvent event() { return event; }

        @Override public boolean cancelled() { return event.isCancelled(); }
        @Override public void cancel() { event.setCancelled(true); }

        public static BukkitClick of(InventoryClickEvent event, PaperComponent component) {
            return new BukkitClick(
                    event,
                    component,
                    PlayerUser.of((Player) event.getWhoClicked()),
                    BukkitSlot.of(event::getCurrentItem, event::setCurrentItem),
                    BukkitSlot.of(event::getCursor, event.getView()::setCursor),
                    event.isLeftClick(), event.isRightClick(), event.isShiftClick()
            );
        }
    }
}
