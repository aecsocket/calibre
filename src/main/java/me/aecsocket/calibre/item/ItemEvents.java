package me.aecsocket.calibre.item;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.calibre.util.itemuser.PlayerItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Contains all generic item events that a {@link me.aecsocket.calibre.item.system.CalibreSystem} can listen to.
 */
public final class ItemEvents {
    private ItemEvents() {}

    // --- BASE ---

    /**
     * The base class for all item events.
     */
    public interface Event {
        ItemStack getItemStack();
        EquipmentSlot getSlot();
    }

    /**
     * The base class for all item events involving an item user.
     */
    public interface UserEvent extends Event {
        ItemUser getUser();
    }

    /**
     * An event which was called by a system.
     * @param <S> The system type.
     */
    public interface SystemEvent<S extends CalibreSystem<?>> {
        S getSystem();
    }

    /**
     * Simple implementation of {@link UserEvent}.
     */
    public static class BaseEvent implements UserEvent {
        private final ItemStack itemStack;
        private final EquipmentSlot slot;
        private final ItemUser user;

        public BaseEvent(ItemStack itemStack, EquipmentSlot slot, ItemUser user) {
            this.itemStack = itemStack;
            this.slot = slot;
            this.user = user;
        }

        @Override public ItemStack getItemStack() { return itemStack; }
        @Override public EquipmentSlot getSlot() { return slot; }
        @Override public ItemUser getUser() { return user; }
    }


    // --- EVENTS ---

    /**
     * Runs when an item is being created with {@link me.aecsocket.calibre.item.component.CalibreComponent#createItem(Player, int)}.
     */
    public static class ItemCreation {
        private final @Nullable Player player;
        private final int amount;
        private final ItemStack item;
        private final ItemMeta meta;
        private final List<String> sections;

        public ItemCreation(@Nullable Player player, int amount, ItemStack item, ItemMeta meta, List<String> sections) {
            this.player = player;
            this.amount = amount;
            this.item = item;
            this.meta = meta;
            this.sections = sections;
        }

        public Player getPlayer() { return player; }
        public int getAmount() { return amount; }
        public ItemStack getItem() { return item; }
        public ItemMeta getMeta() { return meta; }
        public List<String> getSections() { return sections; }
    }

    /**
     * Runs when an item is being updated to a user with {@link me.aecsocket.calibre.item.component.CalibreComponent#updateItem(ItemUser, EquipmentSlot, boolean)}.
     */
    public static class ItemUpdate extends BaseEvent {
        private boolean hidden;

        public ItemUpdate(ItemStack itemStack, EquipmentSlot slot, ItemUser user, boolean hidden) {
            super(itemStack, slot, user);
            this.hidden = hidden;
        }

        public boolean isHidden() { return hidden; }
        public void setHidden(boolean hidden) { this.hidden = hidden; }
    }


    /**
     * Runs when an item is equipped by a player in any {@link EquipmentSlot}.
     * Note that this event may not be run on the main thread, so if you need to access the Bukkit API, check:
     * <code>event.getTickContext().getLoop() instanceof SchedulerLoop</code>
     */
    public static class Equip extends BaseEvent {
        private final TickContext tickContext;

        public Equip(ItemStack itemStack, EquipmentSlot slot, ItemUser user, TickContext tickContext) {
            super(itemStack, slot, user);
            this.tickContext = tickContext;
        }

        public TickContext getTickContext() { return tickContext; }
    }

    /**
     * Runs when an item is left or right clicked.
     */
    public static class Interact extends BaseEvent {
        private final BlockFace clickedFace;
        private final Block clickedBlock;
        private final boolean rightClick;

        public Interact(ItemStack itemStack, EquipmentSlot slot, ItemUser user, BlockFace clickedFace, Block clickedBlock, boolean rightClick) {
            super(itemStack, slot, user);
            this.clickedFace = clickedFace;
            this.clickedBlock = clickedBlock;
            this.rightClick = rightClick;
        }

        public BlockFace getClickedFace() { return clickedFace; }
        public Block getClickedBlock() { return clickedBlock; }
        public boolean isRightClick() { return rightClick; }
    }

    /**
     * Bukkit event-based version of {@link Interact}.
     */
    public static class BukkitInteract extends Interact {
        private final PlayerInteractEvent bukkitEvent;

        public BukkitInteract(CalibrePlugin plugin, PlayerInteractEvent bukkitEvent) {
            super(bukkitEvent.getItem(), bukkitEvent.getHand(), new PlayerItemUser(plugin, bukkitEvent.getPlayer()),
                    bukkitEvent.getBlockFace(), bukkitEvent.getClickedBlock(), Utils.isRightClick(bukkitEvent));
            this.bukkitEvent = bukkitEvent;
        }

        public PlayerInteractEvent getBukkitEvent() { return bukkitEvent; }

        public Interact toRaw() {
            return new Interact(
                    getItemStack(),
                    getSlot(),
                    getUser(),
                    getClickedFace(),
                    getClickedBlock(),
                    isRightClick());
        }
    }

    /**
     * Runs when an item is swapped from the main to offhand and vice versa.
     */
    public static class SwapHands extends BaseEvent {
        public SwapHands(ItemStack itemStack, EquipmentSlot fromSlot, ItemUser user) {
            super(itemStack, fromSlot, user);
        }
    }

    /**
     * Bukkit event-based version of {@link SwapHands}.
     */
    public static class BukkitSwapHands extends SwapHands {
        private final PlayerSwapHandItemsEvent bukkitEvent;

        public BukkitSwapHands(CalibrePlugin plugin, PlayerSwapHandItemsEvent bukkitEvent, boolean fromMainHand) {
            super(
                    fromMainHand ? bukkitEvent.getOffHandItem() : bukkitEvent.getMainHandItem(),
                    fromMainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                    new PlayerItemUser(plugin, bukkitEvent.getPlayer()));
            this.bukkitEvent = bukkitEvent;
        }

        public PlayerSwapHandItemsEvent getBukkitEvent() { return bukkitEvent; }

        public SwapHands toRaw() {
            return new SwapHands(
                    getItemStack(),
                    getSlot(),
                    getUser());
        }
    }

    /**
     * Runs when an entity is damaged by another entity with an item.
     */
    public static class Damage extends BaseEvent {
        private final Entity victim;
        private final double damage;
        private final double finalDamage;
        private final EntityDamageEvent.DamageCause cause;

        public Damage(ItemStack itemStack, EquipmentSlot slot, ItemUser user, Entity victim, double damage, double finalDamage, EntityDamageEvent.DamageCause cause) {
            super(itemStack, slot, user);
            this.victim = victim;
            this.damage = damage;
            this.finalDamage = finalDamage;
            this.cause = cause;
        }

        public Entity getVictim() { return victim; }
        public double getDamage() { return damage; }
        public double getFinalDamage() { return finalDamage; }
        public EntityDamageEvent.DamageCause getCause() { return cause; }
    }

    /**
     * Bukkit event-based version of {@link Damage}.
     */
    public static class BukkitDamage extends Damage {
        private final EntityDamageByEntityEvent bukkitEvent;

        public BukkitDamage(CalibrePlugin plugin, EntityDamageByEntityEvent bukkitEvent, boolean mainHand) {
            super(
                    mainHand
                        ? ((LivingEntity) bukkitEvent.getDamager()).getEquipment().getItemInMainHand()
                        :  ((LivingEntity) bukkitEvent.getDamager()).getEquipment().getItemInOffHand(),
                    mainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                    ItemUser.ofDefault(plugin, bukkitEvent.getDamager()), bukkitEvent.getEntity(),
                    bukkitEvent.getDamage(), bukkitEvent.getFinalDamage(), bukkitEvent.getCause());
            this.bukkitEvent = bukkitEvent;
        }

        public EntityDamageByEntityEvent getBukkitEvent() { return bukkitEvent; }

        public Damage toRaw() {
            return new Damage(
                    getItemStack(),
                    getSlot(),
                    getUser(),
                    getVictim(),
                    getDamage(),
                    getFinalDamage(),
                    getCause());
        }
    }

    /**
     * Runs when an item is equipped and held.
     */
    public static class Draw extends BaseEvent {
        private final int itemSlot;

        public Draw(ItemStack itemStack, EquipmentSlot slot, ItemUser user, int itemSlot) {
            super(itemStack, slot, user);
            this.itemSlot = itemSlot;
        }

        public int getItemSlot() { return itemSlot; }
    }

    /**
     * Bukkit event-based version of {@link Draw}.
     */
    public static class BukkitDraw extends Draw {
        private final PlayerItemHeldEvent bukkitEvent;

        public BukkitDraw(CalibrePlugin plugin, PlayerItemHeldEvent bukkitEvent) {
            super(
                    bukkitEvent.getPlayer().getInventory().getItem(bukkitEvent.getNewSlot()),
                    EquipmentSlot.HAND,
                    new PlayerItemUser(plugin, bukkitEvent.getPlayer()),
                    bukkitEvent.getNewSlot());
            this.bukkitEvent = bukkitEvent;
        }

        public PlayerItemHeldEvent getBukkitEvent() { return bukkitEvent; }

        public Draw toRaw() {
            return new Draw(
                    getItemStack(),
                    getSlot(),
                    getUser(),
                    getItemSlot());
        }
    }

    /**
     * Runs when an item is unequipped from a hand.
     */
    public static class Holster extends BaseEvent {
        private final int itemSlot;

        public Holster(ItemStack itemStack, EquipmentSlot slot, ItemUser user, int itemSlot) {
            super(itemStack, slot, user);
            this.itemSlot = itemSlot;
        }

        public int getItemSlot() { return itemSlot; }
    }
}
