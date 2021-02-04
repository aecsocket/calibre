package me.aecsocket.calibre.world.slot;

import me.aecsocket.calibre.world.Item;

public interface ItemSlot<I extends Item> {
    I get();
    void set(I item);
}
