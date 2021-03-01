package com.gitlab.aecsocket.calibre.core.world.slot;

import com.gitlab.aecsocket.calibre.core.world.item.Item;

public interface HandSlot<I extends Item> extends EquippableSlot<I> {
    boolean main();
    HandSlot<I> opposite();
}