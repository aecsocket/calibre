package me.aecsocket.calibre.util;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.CalibreItem;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a linked pair of {@link ItemStack} and {@link CalibreItem}, which map to each other.
 */
public class ItemRepresentation {
    private ItemStack stack;
    private CalibreItem item;

    public ItemRepresentation(ItemStack stack, CalibreItem item) {
        set(stack, item);
    }

    public ItemRepresentation(ItemStack stack, CalibrePlugin plugin) {
        set(stack, plugin);
    }

    public ItemRepresentation() {}

    public ItemStack getStack() { return stack; }
    public void setStack(ItemStack stack) { this.stack = stack; }

    public CalibreItem getItem() { return item; }
    public void setItem(CalibreItem item) { this.item = item; }

    public void set(ItemStack stack, CalibreItem item) {
        this.stack = stack;
        this.item = item;
    }

    public void set(ItemStack stack, CalibrePlugin plugin) {
        this.stack = stack;
        item = plugin.getItem(stack, CalibreItem.class);
    }
}
