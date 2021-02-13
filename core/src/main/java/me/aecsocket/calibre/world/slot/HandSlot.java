package me.aecsocket.calibre.world.slot;

import me.aecsocket.calibre.world.Item;

public interface HandSlot<I extends Item> extends EquippableSlot<I> {
    boolean main();
    HandSlot<I> opposite();
}
