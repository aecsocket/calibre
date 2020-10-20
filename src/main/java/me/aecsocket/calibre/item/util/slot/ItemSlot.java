package me.aecsocket.calibre.item.util.slot;

import org.bukkit.inventory.ItemStack;

// todo docs on alll this stuff
public interface ItemSlot {
    ItemStack get();
    void set(ItemStack item);
}
