package me.aecsocket.calibre.item;

import me.aecsocket.unifiedframework.util.Utils;
import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Contains all generic item events that a {@link me.aecsocket.calibre.item.system.CalibreSystem} can listen to.
 */
public final class ItemEvents {
    private ItemEvents() {}

    // --- BASE ---

    /**
     * The base class for all item events.
     * @param <T> The listener type.
     */
    public static abstract class Event<T> implements me.aecsocket.unifiedframework.event.Event<T> {
        private final ItemStack itemStack;
        private final EquipmentSlot slot;

        public Event(ItemStack itemStack, EquipmentSlot slot) {
            this.itemStack = itemStack;
            this.slot = slot;
        }

        public ItemStack getItemStack() { return itemStack; }
        public EquipmentSlot getSlot() { return slot; }
    }

    /**
     * The base class for all item events involving a player.
     * @param <T> The listener type.
     */
    public static abstract class PlayerEvent<T> extends Event<T> {
        private final Player player;

        public PlayerEvent(ItemStack itemStack, EquipmentSlot slot, Player player) {
            super(itemStack, slot);
            this.player = player;
        }

        public Player getPlayer() { return player; }
    }

    /**
     * The base class for all item events involving an entity.
     * @param <T> The listener type.
     */
    public static abstract class EntityEvent<T> extends Event<T> {
        private final Entity entity;

        public EntityEvent(ItemStack itemStack, EquipmentSlot slot, Entity entity) {
            super(itemStack, slot);
            this.entity = entity;
        }

        public Entity getEntity() { return entity; }
    }


    // --- EVENTS ---


    /**
     * Runs when an item is held in either the main or offhand.
     */
    public static class Hold extends PlayerEvent<Hold.Listener> {
        public interface Listener { void onEvent(Hold event); }

        public Hold(ItemStack itemStack, EquipmentSlot slot, Player player) {
            super(itemStack, slot, player);
        }

        @Override public void call(Listener listener) { listener.onEvent(this); }
    }

    /**
     * Runs when an item is left or right clicked.
     */
    public static class Interact extends PlayerEvent<Interact.Listener> {
        public interface Listener { void onEvent(Interact event); }

        private final BlockFace clickedFace;
        private final Block clickedBlock;
        private final boolean rightClick;

        public Interact(ItemStack itemStack, EquipmentSlot slot, Player player, BlockFace clickedFace, Block clickedBlock, boolean rightClick) {
            super(itemStack, slot, player);
            this.clickedFace = clickedFace;
            this.clickedBlock = clickedBlock;
            this.rightClick = rightClick;
        }

        public BlockFace getClickedFace() { return clickedFace; }
        public Block getClickedBlock() { return clickedBlock; }
        public boolean isRightClick() { return rightClick; }

        @Override public void call(Listener listener) { listener.onEvent(this); }
    }

    /**
     * Bukkit event-based version of {@link Interact}.
     */
    public static class BukkitInteract extends PlayerEvent<BukkitInteract.Listener> {
        public interface Listener { void onEvent(BukkitInteract event); }

        private final PlayerInteractEvent bukkitEvent;

        public BukkitInteract(PlayerInteractEvent bukkitEvent) {
            super(bukkitEvent.getItem(), bukkitEvent.getHand(), bukkitEvent.getPlayer());
            this.bukkitEvent = bukkitEvent;
        }

        public PlayerInteractEvent getBukkitEvent() { return bukkitEvent; }
        public EquipmentSlot getSlot() { return bukkitEvent.getHand(); }
        public BlockFace getClickedFace() { return bukkitEvent.getBlockFace(); }
        public Block getClickedBlock() { return bukkitEvent.getClickedBlock(); }
        public boolean isRightClick() { return Utils.isRightClick(bukkitEvent); }

        @Override public void call(Listener listener) { listener.onEvent(this); }

        public Interact toRaw() {
            return new Interact(
                    getItemStack(),
                    getSlot(),
                    getPlayer(),
                    getClickedFace(),
                    getClickedBlock(),
                    isRightClick());
        }
    }

    /**
     * Runs when an item is swapped from the main to offhand and vice versa.
     */
    public static class SwapHands extends PlayerEvent<SwapHands.Listener> {
        public interface Listener { void onEvent(SwapHands event); }

        public SwapHands(ItemStack itemStack, EquipmentSlot fromSlot, Player player) {
            super(itemStack, fromSlot, player);
        }

        @Override public void call(Listener listener) { listener.onEvent(this); }
    }

    /**
     * Bukkit event-based version of {@link SwapHands}.
     */
    public static class BukkitSwapHands extends PlayerEvent<BukkitSwapHands.Listener> {
        public interface Listener { void onEvent(BukkitSwapHands event); }

        private final PlayerSwapHandItemsEvent bukkitEvent;

        public BukkitSwapHands(PlayerSwapHandItemsEvent bukkitEvent, boolean fromMainHand) {
            super(
                    fromMainHand ? bukkitEvent.getOffHandItem() : bukkitEvent.getMainHandItem(),
                    fromMainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                    bukkitEvent.getPlayer());
            this.bukkitEvent = bukkitEvent;
        }

        public PlayerSwapHandItemsEvent getBukkitEvent() { return bukkitEvent; }

        @Override public void call(Listener listener) { listener.onEvent(this); }

        public SwapHands toRaw() {
            return new SwapHands(
                    getItemStack(),
                    getSlot(),
                    getPlayer());
        }
    }

    /**
     * Runs when an entity is damaged by another entity with an item.
     */
    public static class Damage extends Event<Damage.Listener> {
        public interface Listener { void onEvent(Damage event); }

        private final Entity victim;
        private final LivingEntity damager;
        private final double damage;
        private final double finalDamage;
        private final EntityDamageEvent.DamageCause cause;

        public Damage(ItemStack itemStack, EquipmentSlot slot, Entity victim, LivingEntity damager, double damage, double finalDamage, EntityDamageEvent.DamageCause cause) {
            super(itemStack, slot);
            this.victim = victim;
            this.damager = damager;
            this.damage = damage;
            this.finalDamage = finalDamage;
            this.cause = cause;
        }

        public Entity getVictim() { return victim; }
        public LivingEntity getDamager() { return damager; }
        public double getDamage() { return damage; }
        public double getFinalDamage() { return finalDamage; }
        public EntityDamageEvent.DamageCause getCause() { return cause; }

        @Override public void call(Listener listener) { listener.onEvent(this); }
    }

    public static class BukkitDamage extends Event<BukkitDamage.Listener> {
        public interface Listener { void onEvent(BukkitDamage event); }

        private final EntityDamageByEntityEvent bukkitEvent;

        public BukkitDamage(EntityDamageByEntityEvent bukkitEvent, boolean mainHand) {
            super(
                    bukkitEvent.getDamager() instanceof LivingEntity
                            ? mainHand
                                    ? ((LivingEntity) bukkitEvent.getDamager()).getEquipment().getItemInMainHand()
                                    :  ((LivingEntity) bukkitEvent.getDamager()).getEquipment().getItemInOffHand()
                            : null,
                    mainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
            Validate.isTrue(bukkitEvent.getDamager() instanceof LivingEntity, "Damager is not of type LivingEntity");
            this.bukkitEvent = bukkitEvent;
        }

        public EntityDamageByEntityEvent getBukkitEvent() { return bukkitEvent; }
        public Entity getVictim() { return bukkitEvent.getEntity(); }
        public LivingEntity getDamager() { return (LivingEntity) bukkitEvent.getDamager(); }
        public double getDamage() { return bukkitEvent.getDamage(); }
        public double getFinalDamage() { return bukkitEvent.getFinalDamage(); }
        public EntityDamageEvent.DamageCause getCause() { return bukkitEvent.getCause(); }

        @Override public void call(Listener listener) { listener.onEvent(this); }

        public Damage toRaw() {
            return new Damage(
                    getItemStack(),
                    getSlot(),
                    getVictim(),
                    getDamager(),
                    getDamage(),
                    getFinalDamage(),
                    getCause());
        }
    }
}
