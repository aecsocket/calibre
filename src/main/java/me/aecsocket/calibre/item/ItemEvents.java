package me.aecsocket.calibre.item;

import me.aecsocket.unifiedframework.event.Event;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Contains all generic item events that a {@link me.aecsocket.calibre.item.system.CalibreSystem} can listen to.
 */
public final class ItemEvents {
    private ItemEvents() {}

    /**
     * The base class for all item events.
     * @param <T> The listener type.
     */
    public static abstract class Event<T> implements me.aecsocket.unifiedframework.event.Event<T> {
        private final ItemStack itemStack;

        public Event(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        public ItemStack getItemStack() { return itemStack; }
    }

    /**
     * The base class for all item events involving a player.
     * @param <T> The listener type.
     */
    public static abstract class PlayerEvent<T> extends Event<T> {
        private final Player player;

        public PlayerEvent(ItemStack itemStack, Player player) {
            super(itemStack);
            this.player = player;
        }

        public Player getPlayer() { return player; }
    }

    /**
     * Runs when an item is held in either the main or offhand.
     */
    public static class Hold extends PlayerEvent<Hold.Listener> {
        public interface Listener { void onHold(
                ItemStack itemStack,
                Player player,
                EquipmentSlot hand); }

        private final EquipmentSlot hand;

        public Hold(ItemStack itemStack, Player player, EquipmentSlot hand) {
            super(itemStack, player);
            this.hand = hand;
        }

        public EquipmentSlot getHand() { return hand; }

        @Override public void call(Listener listener) { listener.onHold(getItemStack(), getPlayer(), hand); }
    }

    /**
     * Runs when an item is left or right clicked.
     */
    public static class Interact extends PlayerEvent<Interact.Listener> {
        public interface Listener { void onInteract(
                ItemStack itemStack,
                Player player,
                EquipmentSlot hand,
                BlockFace clickedFace,
                Block clickedBlock); }

        private final EquipmentSlot hand;
        private final BlockFace clickedFace;
        private final Block clickedBlock;

        public Interact(ItemStack itemStack, Player player, EquipmentSlot hand, BlockFace clickedFace, Block clickedBlock) {
            super(itemStack, player);
            this.hand = hand;
            this.clickedFace = clickedFace;
            this.clickedBlock = clickedBlock;
        }

        public EquipmentSlot getHand() { return hand; }
        public BlockFace getClickedFace() { return clickedFace; }
        public Block getClickedBlock() { return clickedBlock; }

        @Override public void call(Listener listener) { listener.onInteract(getItemStack(), getPlayer(), hand, clickedFace, clickedBlock); }
    }

    /**
     * Bukkit event-based version of {@link Interact}.
     */
    public static class BukkitInteract extends PlayerEvent<BukkitInteract.Listener> {
        public interface Listener { void onInteract(
                ItemStack itemStack,
                Player player,
                EquipmentSlot hand,
                BlockFace clickedFace,
                Block clickedBlock,
                PlayerInteractEvent bukkitEvent); }

        private final PlayerInteractEvent bukkitEvent;

        public BukkitInteract(PlayerInteractEvent bukkitEvent) {
            super(bukkitEvent.getItem(), bukkitEvent.getPlayer());
            this.bukkitEvent = bukkitEvent;
        }

        public PlayerInteractEvent getBukkitEvent() { return bukkitEvent; }
        public EquipmentSlot getHand() { return bukkitEvent.getHand(); }
        public BlockFace getClickedFace() { return bukkitEvent.getBlockFace(); }
        public Block getClickedBlock() { return bukkitEvent.getClickedBlock(); }

        @Override public void call(Listener listener) { listener.onInteract(getItemStack(), getPlayer(), getHand(), getClickedFace(), getClickedBlock(), bukkitEvent); }
    }
}
