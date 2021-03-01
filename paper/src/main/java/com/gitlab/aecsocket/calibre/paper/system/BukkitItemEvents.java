package com.gitlab.aecsocket.calibre.paper.system;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.system.ItemEvents;
import com.gitlab.aecsocket.calibre.paper.util.CalibrePlayerData;
import com.gitlab.aecsocket.calibre.core.world.slot.EquippableSlot;
import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.slot.EntityEquipmentSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.slot.InventorySlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.PlayerUser;
import com.gitlab.aecsocket.calibre.paper.component.PaperComponent;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.calibre.paper.wrapper.slot.BukkitSlot;
import com.gitlab.aecsocket.unifiedframework.core.event.Cancellable;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3I;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class BukkitItemEvents {
    private BukkitItemEvents() {}

    public static class Base extends ItemEvents.Base<BukkitItem> {
        public Base(CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot) {
            super(component, user, slot);
        }
    }

    public static class BukkitEquipped extends Base implements ItemEvents.Equipped<BukkitItem> {
        private final TickContext tickContext;

        public BukkitEquipped(CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, TickContext tickContext) {
            super(component, user, slot);
            this.tickContext = tickContext;
        }

        @Override public TickContext tickContext() { return tickContext; }

        public static BukkitEquipped of(PaperComponent component, Player player, EquipmentSlot slot, TickContext tickContext) {
            return new BukkitEquipped(component, PlayerUser.of(player), EntityEquipmentSlot.of(player, slot), tickContext);
        }
    }

    public static class BukkitJump extends Base implements ItemEvents.Jump<BukkitItem> {
        private final PlayerJumpEvent event;

        public BukkitJump(PlayerJumpEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot) {
            super(component, user, slot);
            this.event = event;
        }

        public PlayerJumpEvent event() { return event; }

        @Override public boolean cancelled() { return event.isCancelled(); }
        @Override public void cancel() { event.setCancelled(true); }

        public static BukkitJump of(PlayerJumpEvent event, PaperComponent component, EquipmentSlot slot) {
            Player player = event.getPlayer();
            return new BukkitJump(
                    event,
                    component,
                    PlayerUser.of(player),
                    EntityEquipmentSlot.of(player, slot)
            );
        }
    }

    public static class BukkitToggleSprint extends Base implements ItemEvents.ToggleSprint<BukkitItem> {
        private final PlayerToggleSprintEvent event;
        private final boolean sprinting;

        public BukkitToggleSprint(PlayerToggleSprintEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, boolean sprinting) {
            super(component, user, slot);
            this.event = event;
            this.sprinting = sprinting;
        }

        public PlayerToggleSprintEvent event() { return event; }
        @Override public boolean sprinting() { return sprinting; }

        public static BukkitToggleSprint of(PlayerToggleSprintEvent event, PaperComponent component, EquipmentSlot slot) {
            Player player = event.getPlayer();
            return new BukkitToggleSprint(
                    event,
                    component,
                    PlayerUser.of(player),
                    EntityEquipmentSlot.of(player, slot),
                    event.isSprinting()
            );
        }
    }

    public static class BukkitScroll extends Base implements ItemEvents.Scroll<BukkitItem> {
        private final int length;

        public BukkitScroll(CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, int length) {
            super(component, user, slot);
            this.length = length;
        }

        @Override public int length() { return length; }

        public static BukkitScroll of(PlayerItemHeldEvent event, PaperComponent component) {
            Player player = event.getPlayer();
            return new BukkitScroll(
                    component,
                    PlayerUser.of(player),
                    InventorySlot.of(player.getInventory(), event.getPreviousSlot()),
                    BukkitUtils.scrollDistance(event)
            );
        }
    }

    public static class BukkitSwitch extends Base implements ItemEvents.Switch<BukkitItem> {
        private final int position;
        private boolean cancelled;

        public BukkitSwitch(CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, int position) {
            super(component, user, slot);
            this.position = position;
        }

        @Override public int position() { return position; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancel() { cancelled = true; }
    }

    public static class BukkitSwapHand extends Base implements ItemEvents.SwapHand<BukkitItem> {
        private final PlayerSwapHandItemsEvent event;
        private final ItemSlot<BukkitItem> offhand;

        public BukkitSwapHand(PlayerSwapHandItemsEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, ItemSlot<BukkitItem> offhand) {
            super(component, user, slot);
            this.event = event;
            this.offhand = offhand;
        }

        public PlayerSwapHandItemsEvent event() { return event; }
        @Override public ItemSlot<BukkitItem> offhand() { return offhand; }

        @Override public boolean cancelled() { return event.isCancelled(); }
        @Override public void cancel() { event.setCancelled(true); }

        public static BukkitSwapHand of(PlayerSwapHandItemsEvent event, PaperComponent component, EquipmentSlot slot) {
            Player player = event.getPlayer();
            return new BukkitSwapHand(
                    event,
                    component,
                    PlayerUser.of(player),
                    EntityEquipmentSlot.of(player, slot),
                    EntityEquipmentSlot.of(player, slot == EquipmentSlot.HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND)
            );
        }
    }

    public static class BukkitDrop extends Base implements ItemEvents.Drop<BukkitItem> {
        private final PlayerDropItemEvent event;

        public BukkitDrop(PlayerDropItemEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot) {
            super(component, user, slot);
            this.event = event;
        }

        public PlayerDropItemEvent event() { return event; }

        @Override public boolean cancelled() { return event.isCancelled(); }
        @Override public void cancel() {
            CalibrePlugin plugin = CalibrePlugin.instance();
            Item drop = event.getItemDrop();
            ItemStack item = drop.getItemStack();
            plugin.itemManager().hide(item, false);
            drop.setItemStack(item);
            event.setCancelled(true);

            if (user() instanceof PlayerUser) {
                CalibrePlayerData data = plugin.playerData(((PlayerUser) user()).entity());
                if (data.animation() != null)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> data.animation().apply(), 2);
            }
        }

        public static BukkitDrop of(PlayerDropItemEvent event, PaperComponent component, boolean equipped) {
            Player player = event.getPlayer();
            Item drop = event.getItemDrop();

            class ItemDropSlot implements BukkitSlot, EquippableSlot<BukkitItem> {
                @Override public ItemStack bukkitGet() { return drop.getItemStack(); }
                @Override public void bukkitSet(ItemStack item) {
                    drop.setItemStack(item);
                }
                @Override public boolean equipped() { return equipped; }
            }

            return new BukkitDrop(
                    event,
                    component,
                    PlayerUser.of(player),
                    new ItemDropSlot()
            );
        }
    }

    public static class BukkitItemClick extends Base implements ItemEvents.ItemClick<BukkitItem> {
        private final InventoryClickEvent event;
        private final ItemSlot<BukkitItem> cursor;
        private final boolean leftClick, rightClick, shiftClick;

        public BukkitItemClick(InventoryClickEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, ItemSlot<BukkitItem> cursor, boolean leftClick, boolean rightClick, boolean shiftClick) {
            super(component, user, slot);
            this.event = event;
            this.cursor = cursor;
            this.leftClick = leftClick;
            this.rightClick = rightClick;
            this.shiftClick = shiftClick;
        }

        public InventoryClickEvent event() { return event; }
        @Override public ItemSlot<BukkitItem> cursor() { return cursor; }
        @Override public boolean leftClick() { return leftClick; }
        @Override public boolean rightClick() { return rightClick; }
        @Override public boolean shiftClick() { return shiftClick; }

        @Override public boolean cancelled() { return event.isCancelled(); }
        @Override public void cancel() { event.setCancelled(true); }

        public static BukkitItemClick of(InventoryClickEvent event, PaperComponent component) {
            return new BukkitItemClick(
                    event,
                    component,
                    PlayerUser.of((Player) event.getWhoClicked()),
                    BukkitSlot.of(event::getCurrentItem, event::setCurrentItem),
                    BukkitSlot.of(event::getCursor, event.getView()::setCursor),
                    event.isLeftClick(), event.isRightClick(), event.isShiftClick()
            );
        }
    }

    public static class BukkitBreakBlock extends Base implements ItemEvents.BreakBlock<BukkitItem> {
        private final BlockBreakEvent event;
        private final Vector3I blockPosition;

        public BukkitBreakBlock(BlockBreakEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, Vector3I blockPosition) {
            super(component, user, slot);
            this.event = event;
            this.blockPosition = blockPosition;
        }

        public BlockBreakEvent event() { return event; }
        @Override public Vector3I blockPosition() { return blockPosition; }

        @Override public boolean cancelled() { return event.isCancelled(); }
        @Override public void cancel() { event.setCancelled(true); }

        public static BukkitBreakBlock of(BlockBreakEvent event, PaperComponent component, EquipmentSlot slot) {
            Player player = event.getPlayer();
            Location location = event.getBlock().getLocation();
            return new BukkitBreakBlock(
                    event,
                    component,
                    PlayerUser.of(player),
                    EntityEquipmentSlot.of(player, slot),
                    new Vector3I(location.getBlockX(), location.getBlockY(), location.getBlockZ())
            );
        }
    }

    public static class BukkitPlaceBlock extends Base implements ItemEvents.PlaceBlock<BukkitItem> {
        private final BlockPlaceEvent event;

        public BukkitPlaceBlock(BlockPlaceEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot) {
            super(component, user, slot);
            this.event = event;
        }

        public BlockPlaceEvent event() { return event; }

        @Override public boolean cancelled() { return event.isCancelled(); }
        @Override public void cancel() { event.setCancelled(true); }

        public static BukkitPlaceBlock of(BlockPlaceEvent event, PaperComponent component, EquipmentSlot slot) {
            Player player = event.getPlayer();
            return new BukkitPlaceBlock(
                    event,
                    component,
                    PlayerUser.of(player),
                    EntityEquipmentSlot.of(player, slot)
            );
        }
    }

    public static class BukkitDeath extends Base implements ItemEvents.Death<BukkitItem> {
        private final EntityDeathEvent event;

        public BukkitDeath(EntityDeathEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot) {
            super(component, user, slot);
            this.event = event;
        }

        public EntityDeathEvent event() { return event; }

        public static BukkitDeath of(EntityDeathEvent event, PaperComponent component, EquipmentSlot slot) {
            return new BukkitDeath(
                    event,
                    component,
                    EntityUser.autoOf(event.getEntity()),
                    EntityEquipmentSlot.of(event.getEntity(), slot)
            );
        }
    }


    public static class PacketEvent implements Cancellable {
        private final ItemStack item;
        private final PaperComponent component;
        private final Player viewer;
        private boolean cancelled;

        public PacketEvent(ItemStack item, PaperComponent component, Player viewer) {
            this.item = item;
            this.component = component;
            this.viewer = viewer;
        }

        public ItemStack item() { return item; }
        public CalibreComponent<BukkitItem> component() { return component; }
        public Player viewer() { return viewer; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancel() { cancelled = true; }
    }

    public static class ShowItem extends PacketEvent {
        private final Entity holder;

        public ShowItem(ItemStack item, PaperComponent component, Player viewer, Entity holder) {
            super(item, component, viewer);
            this.holder = holder;
        }

        public Entity holder() { return holder; }
    }

    public static class Swing extends PacketEvent {
        private final LivingEntity holder;

        public Swing(ItemStack item, PaperComponent component, Player viewer, LivingEntity holder) {
            super(item, component, viewer);
            this.holder = holder;
        }

        public LivingEntity holder() { return holder; }
    }
}
