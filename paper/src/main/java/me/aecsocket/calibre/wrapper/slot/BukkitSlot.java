package me.aecsocket.calibre.wrapper.slot;

import me.aecsocket.calibre.world.ItemSlot;
import me.aecsocket.calibre.wrapper.BukkitItem;
import me.aecsocket.unifiedframework.util.BukkitUtils;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface BukkitSlot extends ItemSlot<BukkitItem> {
    ItemStack bukkitGet();
    void bukkitSet(ItemStack item);

    @Override default BukkitItem get() {
        ItemStack stack = bukkitGet();
        return BukkitUtils.empty(stack) ? null : BukkitItem.of(stack);
    }
    @Override default void set(BukkitItem item) { bukkitSet(item == null ? null : item.item()); }

    static BukkitSlot of(Supplier<ItemStack> get, Consumer<ItemStack> set) {
        return new BukkitSlot() {
            @Override public ItemStack bukkitGet() { return get.get(); }
            @Override public void bukkitSet(ItemStack item) { set.accept(item); }
        };
    }
}
