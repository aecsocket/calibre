package me.aecsocket.calibre.item;

import me.aecsocket.calibre.item.system.CalibreSystem;
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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
     * An event which was called by a system.
     * @param <S> The system type.
     */
    public interface SystemEvent<S extends CalibreSystem<?>> {
        S getSystem();
    }


    // --- EVENTS ---


    /**
     * Runs when an item is equipped by a player in any {@link EquipmentSlot}.
     * Note that this event may not be run on the main thread, so if you need to access the Bukkit API, check:
     * <code>event.getTickContext().getLoop() instanceof SchedulerLoop</code>
     */
    public static class Equip<L extends Equip.Listener> extends PlayerEvent<L> {
        public interface Listener { void onEvent(Equip<?> event); }

        private final TickContext tickContext;

        public Equip(ItemStack itemStack, EquipmentSlot slot, Player player, TickContext tickContext) {
            super(itemStack, slot, player);
            this.tickContext = tickContext;
        }

        public TickContext getTickContext() { return tickContext; }

        @Override public void call(Listener listener) { listener.onEvent(this); }
    }

    /**
     * Runs when an item is left or right clicked.
     */
    public static class Interact<L extends Interact.Listener> extends PlayerEvent<L> {
        public interface Listener { void onEvent(Interact<?> event); }

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
    public static class BukkitInteract<L extends BukkitInteract.Listener> extends Interact<L> {
        public interface Listener extends Interact.Listener { void onEvent(BukkitInteract<?> event); }

        private final PlayerInteractEvent bukkitEvent;

        public BukkitInteract(PlayerInteractEvent bukkitEvent) {
            super(bukkitEvent.getItem(), bukkitEvent.getHand(), bukkitEvent.getPlayer(),
                    bukkitEvent.getBlockFace(), bukkitEvent.getClickedBlock(), Utils.isRightClick(bukkitEvent));
            this.bukkitEvent = bukkitEvent;
        }

        public PlayerInteractEvent getBukkitEvent() { return bukkitEvent; }

        @Override public void call(Listener listener) { listener.onEvent(this); }

        public Interact<?> toRaw() {
            return new Interact<>(
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
    public static class SwapHands<L extends SwapHands.Listener> extends PlayerEvent<L> {
        public interface Listener { void onEvent(SwapHands<?> event); }

        public SwapHands(ItemStack itemStack, EquipmentSlot fromSlot, Player player) {
            super(itemStack, fromSlot, player);
        }

        @Override public void call(Listener listener) { listener.onEvent(this); }
    }

    /**
     * Bukkit event-based version of {@link SwapHands}.
     */
    public static class BukkitSwapHands<L extends BukkitSwapHands.Listener> extends SwapHands<L> {
        public interface Listener extends SwapHands.Listener { void onEvent(BukkitSwapHands<?> event); }

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

        public SwapHands<?> toRaw() {
            return new SwapHands<>(
                    getItemStack(),
                    getSlot(),
                    getPlayer());
        }
    }

    /**
     * Runs when an entity is damaged by another entity with an item.
     */
    public static class Damage<L extends Damage.Listener> extends Event<L> {
        public interface Listener { void onEvent(Damage<?> event); }

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

    public static class BukkitDamage<L extends BukkitDamage.Listener> extends Damage<L> {
        public interface Listener extends Damage.Listener { void onEvent(BukkitDamage<?> event); }

        private final EntityDamageByEntityEvent bukkitEvent;

        public BukkitDamage(EntityDamageByEntityEvent bukkitEvent, boolean mainHand) {
            super(
                    mainHand
                        ? ((LivingEntity) bukkitEvent.getDamager()).getEquipment().getItemInMainHand()
                        :  ((LivingEntity) bukkitEvent.getDamager()).getEquipment().getItemInOffHand(),
                    mainHand ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                    bukkitEvent.getEntity(), (LivingEntity) bukkitEvent.getDamager(),
                    bukkitEvent.getDamage(), bukkitEvent.getFinalDamage(), bukkitEvent.getCause());
            this.bukkitEvent = bukkitEvent;
        }

        public EntityDamageByEntityEvent getBukkitEvent() { return bukkitEvent; }

        @Override public void call(Listener listener) { listener.onEvent(this); }

        public Damage<?> toRaw() {
            return new Damage<>(
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
