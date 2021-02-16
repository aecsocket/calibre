package me.aecsocket.calibre.system;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.PaperComponent;
import me.aecsocket.calibre.util.CalibrePlayer;
import me.aecsocket.calibre.world.slot.EquippableSlot;
import me.aecsocket.calibre.world.slot.ItemSlot;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.calibre.wrapper.slot.BukkitSlot;
import me.aecsocket.calibre.wrapper.slot.EntityEquipmentSlot;
import me.aecsocket.calibre.wrapper.slot.InventorySlot;
import me.aecsocket.calibre.wrapper.user.EntityUser;
import me.aecsocket.calibre.wrapper.user.PlayerUser;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import me.aecsocket.unifiedframework.util.vector.Vector3I;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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

    public static class BukkitInteract extends Base implements ItemEvents.Interact<BukkitItem> {
        private final PlayerInteractEvent event;
        private final int type;

        public BukkitInteract(PlayerInteractEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, int type) {
            super(component, user, slot);
            this.event = event;
            this.type = type;
        }

        public PlayerInteractEvent event() { return event; }
        @Override public int type() { return type; }

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
                CalibrePlayer data = plugin.playerData(((PlayerUser) user()).entity());
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

    public static class BukkitClick extends Base implements ItemEvents.Click<BukkitItem> {
        private final InventoryClickEvent event;
        private final ItemSlot<BukkitItem> cursor;
        private final boolean leftClick, rightClick, shiftClick;

        public BukkitClick(InventoryClickEvent event, CalibreComponent<BukkitItem> component, ItemUser user, ItemSlot<BukkitItem> slot, ItemSlot<BukkitItem> cursor, boolean leftClick, boolean rightClick, boolean shiftClick) {
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