package me.aecsocket.calibre.item.util.slot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface InventoryItemSlot extends ItemSlot {
    Inventory getInventory();
    int getInventorySlot();

    @Override default ItemStack get() { return getInventory().getItem(getInventorySlot()); }
    @Override default void set(ItemStack item) { getInventory().setItem(getInventorySlot(), item); }
}
