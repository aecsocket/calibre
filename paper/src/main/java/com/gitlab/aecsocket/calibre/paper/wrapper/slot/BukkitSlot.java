package com.gitlab.aecsocket.calibre.paper.wrapper.slot;

import com.gitlab.aecsocket.calibre.core.world.slot.ItemSlot;
import com.gitlab.aecsocket.calibre.paper.wrapper.BukkitItem;
import com.gitlab.aecsocket.unifiedframework.paper.util.BukkitUtils;
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
