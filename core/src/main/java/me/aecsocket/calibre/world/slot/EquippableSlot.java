package me.aecsocket.calibre.world.slot;

import me.aecsocket.calibre.world.Item;

public interface EquippableSlot<I extends Item> extends ItemSlot<I> {
    boolean equipped();
}
