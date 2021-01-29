package me.aecsocket.calibre.wrapper.slot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface InventorySlot extends BukkitSlot {
    Inventory inventory();
    int slot();

    default ItemStack bukkitGet() { return inventory().getItem(slot()); }
    default void bukkitSet(ItemStack item) { inventory().setItem(slot(), item); }

    static InventorySlot of(Inventory inventory, int slot) {
        return new InventorySlot() {
            @Override public Inventory inventory() { return inventory; }
            @Override public int slot() { return slot; }
        };
    }
}
